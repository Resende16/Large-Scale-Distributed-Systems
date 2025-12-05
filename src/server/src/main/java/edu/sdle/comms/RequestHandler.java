package edu.sdle.comms;

import edu.sdle.Node;
import edu.sdle.UUIDUtils;
import edu.sdle.hashring.DistributedHashRing;
import edu.sdle.storage.Storage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.*;
import java.util.UUID;
import java.util.concurrent.*;

public class RequestHandler {
    private final Storage storage;
    private final DistributedHashRing hashRing;
    private final ExecutorService backupPool;

    private final ThreadLocal<ZMQ.Socket> socket;
    private final int timeout;
    private final int N;

    public RequestHandler(ZContext context,
                          Storage storage,
                          DistributedHashRing hashRing,
                          ExecutorService backupPool,
                          int timeout, int N) {
        this.storage = storage;
        this.hashRing = hashRing;
        // The backup pool of threads is used for subrequests
        this.backupPool = backupPool;
        this.timeout = timeout;
        this.N = N;

        // Create thread-local socket for subrequests
        socket = ThreadLocal.withInitial(() -> {
            ZMQ.Socket s = context.createSocket(SocketType.DEALER);
            s.setSendTimeOut(timeout);
            s.setReceiveTimeOut(timeout);
            return s;
        });
    }

    public ZMsg handleRequest(ZMsg msg) {
        var typeStr = msg.pop().toString();
        var type = RequestType.fromString(typeStr);

        if (type == null)
            // Invalid type
            return ReplyUtils.failure();

        // Validate message size + hand off to handler
        switch (type) {
            case GET -> {
                if (!msg.isEmpty()) // key
                    return handleGetRequest(msg);
            }
            case PUT -> {
                if (msg.size() > 1) // key + val
                    return handlePutRequest(msg);
            }
        }

        // Otherwise malformed
        return ReplyUtils.failure();
    }

    public ZMsg handleGetRequest(ZMsg msg) {
        var bytes = msg.pop().getData();

        if (bytes.length != 16)
            // Invalid UUID
            return ReplyUtils.failure();

        var key = UUIDUtils.uuidFromBytes(bytes);
        var payload = storage.getAsJson(key);

        if (payload == null || payload.isEmpty())
            return ReplyUtils.failure();
        else
            return ReplyUtils.success(payload);
    }

    public ZMsg handlePutRequest(ZMsg msg) {
        var bytes = msg.pop().getData();

        if (bytes.length != 16)
            // Invalid UUID
            return ReplyUtils.failure();

        var value = msg.pop().toString();
        var key = UUIDUtils.uuidFromBytes(bytes);

        if (storage.putJson(key, value))
            return ReplyUtils.success();
        else
            return ReplyUtils.failure();
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
}