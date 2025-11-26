package edu.sdle;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sdle.comms.RequestType;
import edu.sdle.hashring.DistributedHashRing;
import edu.sdle.storage.FileStorage;
import edu.sdle.storage.Storage;
import org.zeromq.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class Worker implements Runnable {
    private final ZContext context;
    private final ExecutorService backupPool;
    private final int workerIndex;

    private final Storage storage = new FileStorage();
    private final ObjectMapper mapper = new ObjectMapper();
    private final DistributedHashRing hashRing = null;

    private ThreadLocal<ZMQ.Socket> socket;


    private final int N = 1;
    private final int R = 1;
    private final int W = 1;

    public Worker(ZContext context, ExecutorService backupPool, int workerIndex) {
        this.context = context;
        // The backup pool of threads is used for subrequests
        this.backupPool = backupPool;
        this.workerIndex = workerIndex;
    }

    public void run() {
        Thread.currentThread().setName("Worker-" + workerIndex);

        ZMQ.Socket worker = context.createSocket(SocketType.DEALER);
        worker.connect("inproc://backend");
        worker.send("READY"); // signal READY

        // Create thread-local socket for subrequests
        socket = ThreadLocal.withInitial(() ->
                context.createSocket(SocketType.DEALER));

        System.out.println("Worker " + workerIndex + " is listening");

        try {
            while (!Thread.currentThread().isInterrupted()) {
                ZMsg msg = ZMsg.recvMsg(worker);
                byte[] clientId = msg.pop().getData();
                msg.pop();

                var reply = handleRequest(msg);

                // Prepend identity and reply
                reply.push("");
                reply.push(clientId);
                reply.send(worker);
            }
        } catch (Exception e) {
            System.err.println("[" + Thread.currentThread().getName() + "] Worker thread interrupted\n" + e);
        }
    }

    private ZMsg handleRequest(ZMsg msg) {
        var typeStr = msg.pop().toString();
        var type = RequestType.fromString(typeStr);

        if (type == null)
            // Invalid type
            return failureReply();

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
        return failureReply();
    }

    private ZMsg handleCoordinateGetRequest(ZMsg msg) {
        return null;
    }


    private ZMsg handleCoordinatePutRequest(ZMsg msg) {
        return null;
    }


        private ZMsg handleCoordinateGetRequest(UUID key) {
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

    private ZMsg handleCoordinatePutRequest(UUID key) {
        return null;
    }

        // Existence of all fields should be checked by caller
    private ZMsg handleGetRequest(ZMsg msg) {
        var bytes = msg.pop().getData();
        ZMsg reply = new ZMsg();

        if (bytes.length != 16) {
            // Invalid UUID
            reply.add("1");
            return reply;
        }

        UUID key = UUIDUtils.uuidFromBytes(bytes);
        var payload = storage.getAsJson(key);

        if (payload == null || payload.isEmpty())
            reply.add("1"); // Failed
        else {
            reply.add("0"); // Success
            reply.add(payload);
        }
        return reply;
    }

    // Existence of all fields should be checked by caller
    private ZMsg handlePutRequest(ZMsg msg) {
        var bytes = msg.pop().getData();

        if (bytes.length != 16) {
            // Invalid UUID
            ZMsg reply = new ZMsg();
            reply.add("1");
            return reply;
        }

        String value = msg.pop().toString();
        UUID key = UUIDUtils.uuidFromBytes(bytes);
        return handlePutRequest(key, value);
    }

    private ZMsg handleGetRequest(UUID key) {
        ZMsg reply = new ZMsg();
        String payload = storage.getAsJson(key);

        if (payload == null || payload.isEmpty())
            reply.add("1"); // Failed
        else {
            reply.add("0"); // Success
            reply.add(payload);
        }
        return reply;
    }

    private ZMsg handlePutRequest(UUID key, String value) {
        ZMsg reply = new ZMsg();

        if (storage.putJson(key, value))
            reply.add("0");
        else
            reply.add("1");
        return reply;
    }

    private ZMsg makeGetRequest(Node node, UUID key) {
        ZMsg msg = new ZMsg();
        msg.add(RequestType.GET.toString());

        var bytes = UUIDUtils.uuidToBytes(key);
        msg.add(bytes);

        var socket = this.socket.get();
        //if (socket == null)
        //    return null;

        socket.connect(node.getZMQAddress());
        msg.send(socket);

        return ZMsg.recvMsg(socket);
    }

    private ZMsg makePutRequest(Node node, UUID key, String value) {
        ZMsg msg = new ZMsg();
        msg.add(RequestType.PUT.toString());
        msg.add(key.toString());
        msg.add(value);

        var socket = this.socket.get();
        //if (socket == null)
        //    return null;

        socket.connect(node.getZMQAddress());
        msg.send(socket);

        return ZMsg.recvMsg(socket);
    }

    // Represents a failure of some sort, i.e.
    // the request did not succeed
    private static ZMsg failureReply() {
        ZMsg reply = new ZMsg();
        reply.add("1");
        return reply;
    }
}
