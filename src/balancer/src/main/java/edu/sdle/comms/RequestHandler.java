package edu.sdle.comms;

import edu.sdle.Node;
import edu.sdle.UUIDUtils;
import edu.sdle.hashring.DistributedHashRing;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import zmq.socket.reqrep.Rep;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class RequestHandler {
    private final DistributedHashRing hashRing;
    private final ExecutorService backupPool;

    private final ThreadLocal<ZMQ.Socket> socket;
    private final int timeout;
    private final int N;

    public RequestHandler(ZContext context,
                          DistributedHashRing hashRing,
                          ExecutorService backupPool,
                          int timeout, int N) {
        this.hashRing = hashRing;
        // The backup pool of threads is used for subrequests
        this.backupPool = backupPool;
        this.timeout = timeout;
        this.N = N;

        // Create thread-local socket for subrequests
        socket = ThreadLocal.withInitial(() -> {
            ZMQ.Socket s = context.createSocket(SocketType.DEALER);
            // send_timeout = t/N
            s.setSendTimeOut(timeout / N);
            // recv_timeout = t - t/N = t - send_timeout
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
}