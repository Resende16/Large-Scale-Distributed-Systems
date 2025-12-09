package edu.sdle.comms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sdle.Node;
import edu.sdle.ProxyState;
import edu.sdle.UUIDUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class RequestHandler {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProxyState proxyState;
    private final ExecutorService backupPool;
    private final ThreadLocal<ZMQ.Socket> socket;

    public RequestHandler(ZContext context,
                          ProxyState proxyState,
                          ExecutorService backupPool) {
        this.proxyState = proxyState;
        this.backupPool = backupPool;

        // Create thread-local socket for server requests
        socket = ThreadLocal.withInitial(() -> {
            var N = proxyState.getN();
            var tmpN = N == 1 ? 2 : N;
            var timeout = proxyState.getTimeout();

            // Send timeout is set such that a GET can
            // succeed even if N-1 replicas are down
            ZMQ.Socket s = context.createSocket(SocketType.REQ);
            s.setSendTimeOut(timeout / tmpN);
            s.setReceiveTimeOut(timeout - s.getSendTimeOut());
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
                var key = parseKey(msg);
                if (key != null)
                    return handleGetRequest(key);
            }
            case PUT -> {
                var key = parseKey(msg);
                var value = msg.pop();
                if (key != null && value != null)
                    return handlePutRequest(key, value.toString());
            }
            case STATE ->  {
                return handleStateRequest();
            }
        }

        // Otherwise malformed
        return ReplyUtils.failure();
    }

    public UUID parseKey(ZMsg msg) {
        if (msg.isEmpty())
            return null;

        var bytes = msg.pop().getData();
        if (bytes.length != 16)
            // Invalid UUID
            return null;

        return UUIDUtils.uuidFromBytes(bytes);
    }

    public ZMsg makeGetRequest(Node node, UUID key) {
        ZMsg msg = new ZMsg();
        msg.add(RequestType.GET.toString());

        var bytes = UUIDUtils.uuidToBytes(key);
        msg.add(bytes);

        var socket = this.socket.get();
        socket.connect(node.getZMQAddress());
        msg.send(socket);

        return ZMsg.recvMsg(socket);
    }

    public ZMsg makePutRequest(Node node, UUID key, String value) {
        ZMsg msg = new ZMsg();
        msg.add(RequestType.PUT.toString());

        var bytes = UUIDUtils.uuidToBytes(key);
        msg.add(bytes);
        msg.add(value);

        var socket = this.socket.get();
        socket.connect(node.getZMQAddress());
        msg.send(socket);

        return ZMsg.recvMsg(socket);
    }

    public ZMsg handleGetRequest(UUID key) {
        var N = proxyState.getN();
        var timeout = proxyState.getTimeout();
        var hashRing = proxyState.getHashRing();

        List<Node> replicas = hashRing.getNNodes(key, N);
        if (replicas.isEmpty())
            // Hash ring is empty?
            return ReplyUtils.failure();

        long deadline = System.currentTimeMillis() + timeout;

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
        var N = proxyState.getN();
        var timeout = proxyState.getTimeout();
        var hashRing = proxyState.getHashRing();

        List<Node> replicas = hashRing.getNNodes(key, N);
        if (replicas.isEmpty())
            return ReplyUtils.failure();

        CompletionService<ZMsg> completionService =
                new ExecutorCompletionService<>(backupPool);

        // Submit PUT to all replicas concurrently
        for (Node node : replicas)
            completionService.submit(() -> makePutRequest(node, key, value));

        long deadline = System.currentTimeMillis() + timeout;

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
        try {
            var json = MAPPER.writeValueAsString(proxyState);
            return ReplyUtils.success(json);

        } catch (JsonProcessingException _) {
            return ReplyUtils.failure();
        }
    }
}