package edu.sdle.comms;

import edu.sdle.Node;
import edu.sdle.UUIDUtils;
import edu.sdle.crdt.ICRDT;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.hashring.DistributedHashRing;
import edu.sdle.storage.Storage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class RequestHandler {
    private final Storage storage;
    private final DistributedHashRing hashRing;
    private final ExecutorService backupPool;

    private final ThreadLocal<ZMQ.Socket> socket;
    private final int sendTimeout;
    private final int recvTimeout;

    private final int N;
    private final int R;
    private final int W;

    public RequestHandler(ZContext context,
                          Storage storage,
                          DistributedHashRing hashRing,
                          ExecutorService backupPool,
                          int sendTimeout,
                          int recvTimeout,
                          int N, int R, int W) {
        this.storage = storage;
        this.hashRing = hashRing;
        // The backup pool of threads is used for subrequests
        this.backupPool = backupPool;
        // A successful request must take no more
        // than send+recv timeout to complete
        this.sendTimeout = sendTimeout;
        this.recvTimeout = recvTimeout;
        this.N = N;
        this.R = R;
        this.W = W;

        // Create thread-local socket for subrequests
        socket = ThreadLocal.withInitial(() -> {
            ZMQ.Socket s = context.createSocket(SocketType.DEALER);
            s.setSendTimeOut(sendTimeout);
            s.setReceiveTimeOut(recvTimeout);
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
            case COORDINATE_GET -> {
                if (!msg.isEmpty())
                    return handleCoordinateGetRequest(msg);
            }
            case PUT -> {
                if (msg.size() > 1) // key + val
                    return handlePutRequest(msg);
            }
            case COORDINATE_PUT -> {
                if (msg.size() > 1)
                    return handleCoordinatePutRequest(msg);
            }
        }

        // Otherwise malformed
        return ReplyUtils.failure();
    }



    public ZMsg handleCoordinateGetRequest(ZMsg msg) {
        var bytes = msg.pop().getData();

        if (bytes.length != 16)
            return ReplyUtils.failure();

        var key = UUIDUtils.uuidFromBytes(bytes);
        List<Node> replicas = hashRing.getNNodes(key, N);

        if (replicas.isEmpty())
            return ReplyUtils.failure();

        var deadline = System.currentTimeMillis()
                + sendTimeout
                + recvTimeout;

        int index = 0; // next node to contact
        int successes = 0;
        ShoppingList result = null;

        while (
            // Continue if quorum is not met + timeout hasn't
            // exceeded + there are replicas left to try
                successes < R
                        && System.currentTimeMillis() < deadline
                        && index < replicas.size()
        )
        {
            List<Future<ZMsg>> futures = new ArrayList<>();

            // Submit subrequests to backup pool (except 1)
            for (int i = index; i < replicas.size(); i++) {
                Node node = replicas.get(i);
                var future = backupPool.submit(() -> makeGetRequest(node, key));
                futures.add(future);
            }

            // Make 1 subrequest inline as to spare workers
            Node firstNode = replicas.get(index);
            var reply = makeGetRequest(firstNode, key);

            var value = handleGetReply(reply);
            if (value != null) {
                if (result == null)
                    result = value;
                else
                    result.merge(value);
                successes++;
            }

            try {
                // Collect responses from workers
                for (Future<ZMsg> future : futures) {
                    var r = future.get(); // blocks

                    var v = handleGetReply(r);
                    if (v != null) {
                        if (result == null)
                            result = v;
                        else
                            result.merge(v);
                        successes++;
                    }
                }
            } catch (Exception _) {}
        }

        if (successes < R || result == null)
            return ReplyUtils.failure();

        var payload = result.toJson();
        if (payload == null)
            return ReplyUtils.failure();

        return ReplyUtils.success(payload);
    }

    public static class Merger<T extends ICRDT<T>> {
        private T obj;

        public Merger(T obj) {
            this.obj = obj;
        }

        public T get() {
            return obj;
        }

        public void merge(T obj) {
            if (this.obj == null)
                this.obj = obj;
            else
                this.obj.merge(obj);
        }
    }

    public ShoppingList handleGetReply(ZMsg reply) {
        if (!ReplyUtils.didSucceed(reply) || reply.isEmpty())
            return null;

        String payload = reply.pop().toString();
        return ShoppingList.fromJson(payload);
    }


    public boolean isSuccessResponse(ZMsg msg) {
        if (msg == null || msg.isEmpty())
            return false;

        // Assuming first frame indicates success/failure
        return msg.getFirst().toString().equals("SUCCESS");
    }

    public ZMsg resolveVersions(List<ZMsg> responses) {
        return null;
    }





    public ZMsg handleCoordinateGetRequest(UUID key) {
        List<Node> replicas = hashRing.getNNodes(key, N);
        int successes = 0;
        int attempted = 0;
        ZMsg reply = new ZMsg();

        for (Node node : replicas) {
            if (successes >= R)
                break;

            String payload = null;
            attempted++;

            if (payload != null && !payload.isEmpty()) {
                // On first success, add the success signal
                if (successes == 0)
                    reply.add("0");
                reply.add(payload);
                successes++;
            }
            // else failed: try next node until R successes or out of candidates
        }
        // If not enough successes, return a failure signal
        if (successes == 0) {
            reply.add("1"); // failure
        }
        return reply;
    }

    public ZMsg handleCoordinatePutRequest(UUID key) {
        return null;
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
