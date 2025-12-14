package edu.sdle.comms;

import edu.sdle.*;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class RequestHandler {
    private final ProxyState state;
    private final ExecutorService backupPool;
    private final ThreadLocal<ZMQ.Socket> socket;

    public RequestHandler(ZContext context,
                          ProxyState state,
                          ExecutorService backupPool) {
        this.state = state;
        this.backupPool = backupPool;

        // Create thread-local socket for server requests
        socket = ThreadLocal.withInitial(() -> {
            ZMQ.Socket s = context.createSocket(SocketType.REQ);
            s.setReqRelaxed(true);
            s.setLinger(0);

            var N = state.N();
            var tmpN = N == 1 ? 2 : N;
            var timeout = state.timeout();

            // Send timeout is set such that a GET can
            // succeed even if N-1 replicas are down
            s.setReceiveTimeOut(timeout / tmpN);
            s.setSendTimeOut(timeout - s.getReceiveTimeOut());
            return s;
        });
    }

    public ZMsg handleRequest(ZMsg msg) {
        var typeStr = msg.pop().toString();
        var type = RequestType.fromString(typeStr);

        if (type == null)
            // Invalid type
            return ReplyUtils.failure();

        // Parse args + hand off to handler
        switch (type) {
            case GET -> {
                var key = parseUUID(msg);
                if (key != null)
                    return handleGetRequest(key);
            }
            case PUT -> {
                var key = parseUUID(msg);
                var value = msg.pop();
                if (key != null && value != null)
                    return handlePutRequest(key, value.toString());
            }
            case JOIN -> {
                var nodeId = parseUUID(msg);
                var nodeAddr = msg.pop();

                if (nodeId != null && nodeAddr != null) {
                    var node = new Node(nodeId, nodeAddr.toString());
                    return handleJoinRequest(node);
                }
            }
            case LEAVE -> {
                var nodeId = parseUUID(msg);
                var nodeAddr = msg.pop();

                if (nodeId != null && nodeAddr != null) {
                    var node = new Node(nodeId, nodeAddr.toString());
                    return handleLeaveRequest(node);
                }
            }
            case STATE ->  {
                return handleStateRequest();
            }
        }

        // Otherwise malformed
        return ReplyUtils.failure();
    }

    public UUID parseUUID(ZMsg msg) {
        if (msg.isEmpty())
            return null;

        var bytes = msg.pop().getData();
        if (bytes.length != 16)
            // Invalid UUID
            return null;

        return UUIDUtils.uuidFromBytes(bytes);
    }

    public ZMsg makeRequest(Node node, ZMsg msg) {
        var socket = this.socket.get();
        var zmqAddr = node.getZMQAddress();
        socket.connect(zmqAddr);
        msg.send(socket);

        var reply = ZMsg.recvMsg(socket);
        socket.disconnect(zmqAddr);
        return reply;
    }

    public ZMsg makeGetRequest(Node node, UUID key) {
        System.out.println("< GET " + key + " to " + node.getZMQAddress());

        ZMsg msg = new ZMsg();
        msg.add(RequestType.GET.toString());
        msg.add(UUIDUtils.uuidToBytes(key));

        return makeRequest(node, msg);
    }

    public ZMsg makePutRequest(Node node, UUID key, String value) {
        System.out.println("< PUT " + key + " to " + node.getZMQAddress());

        ZMsg msg = new ZMsg();
        msg.add(RequestType.PUT.toString());
        msg.add(UUIDUtils.uuidToBytes(key));
        msg.add(value);

        return makeRequest(node, msg);
    }

    public ZMsg handleGetRequest(UUID key) {
        System.out.println("> GET " + key);

        List<Node> replicas = state.ring().getNNodes(key, state.N());
        if (replicas.isEmpty())
            // Hash ring is empty?
            return ReplyUtils.failure();

        long deadline = System.currentTimeMillis() + state.timeout();

        // Wait for the first successful response (sequential)
        for (Node node : replicas) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0)
                break; // Timed out

            var reply = makeGetRequest(node, key);

            if (ReplyUtils.didSucceed(reply) && !reply.isEmpty()) {
                var payload = reply.pop();
                return ReplyUtils.success(payload);
            }
        }

        return ReplyUtils.failure();
    }

    public ZMsg handlePutRequest(UUID key, String value) {
        System.out.println("> PUT " + key);

        List<Node> replicas = state.ring().getNNodes(key, state.N());
        if (replicas.isEmpty())
            return ReplyUtils.failure();

        CompletionService<ZMsg> completionService =
                new ExecutorCompletionService<>(backupPool);

        // Submit PUT to all replicas concurrently
        for (Node node : replicas)
            completionService.submit(() -> makePutRequest(node, key, value));

        long deadline = System.currentTimeMillis() + state.timeout();

        // Wait for at least one success
        try {
            while (System.currentTimeMillis() < deadline) {
                long remaining = deadline - System.currentTimeMillis();

                Future<ZMsg> completed = completionService.poll(remaining, MILLISECONDS);
                if (completed == null)
                    break;

                var r = completed.get();
                if (ReplyUtils.didSucceed(r))
                    return ReplyUtils.success();
            }
        } catch (InterruptedException | ExecutionException e) {
            //todo: Log
            return ReplyUtils.failure();
        }

        return ReplyUtils.failure();
    }

    public ZMsg handleStateRequest() {
        System.out.println("> STATE ");

        var json = JsonUtils.toJson(state);
        if (json == null)
            return ReplyUtils.failure();

        return ReplyUtils.success(json);
    }

    public ZMsg handleJoinRequest(Node node) {
        System.out.println("> JOIN " + node.getZMQAddress());

        var hashRing = state.ring();

        // Node already in the ring
        if (hashRing.containsNode(node.getId()))
            return ReplyUtils.failure();

        // issuer is the node itself?
        hashRing.addNode(node, node.getId());

        return ReplyUtils.success();
    }

    public ZMsg handleLeaveRequest(Node node) {
        System.out.println("> LEAVE " + node.getZMQAddress());

        var hashRing = state.ring();

        // Node is not in the ring
        if (!hashRing.containsNode(node.getId()))
            return ReplyUtils.failure();

        hashRing.removeNode(node.getId());
        return ReplyUtils.success();
    }
}
