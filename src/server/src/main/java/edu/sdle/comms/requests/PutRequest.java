package edu.sdle.comms.requests;

import edu.sdle.NodeState;
import edu.sdle.comms.ReplyUtils;
import edu.sdle.comms.RequestHandler;
import edu.sdle.hashring.Node;
import edu.sdle.utils.RequestUtils;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.concurrent.*;

import static edu.sdle.hashring.NodeStatus.UP;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class PutRequest extends Request {
    private final ThreadLocal<ZMQ.Socket> socket;
    private final ExecutorService backupPool;

    public PutRequest(ThreadLocal<ZMQ.Socket> socket,
                      ExecutorService backupPool) {
        this.socket = socket;
        this.backupPool = backupPool;
    }

    // Socket used for sending REPLICA_PUT to replicas
    public ZMQ.Socket getSocket() {
        return socket.get();
    }

    @Override
    public ZMsg handle(NodeState state, ZMsg msg) {
        // Parse key and value
        var key = RequestUtils.parseUUID(msg);
        var valueFrame = msg.pop();

        if (key == null || valueFrame == null)
            return ReplyUtils.failure();

        var value = valueFrame.toString();

        //todo: Better logging
        System.out.println("> PUT " + key);

        var replicas = state.ring().getNNodes(key);
        boolean earlySuccess = false;

        // Own node is responsible, store locally
        if (replicas.contains(state.node()))
            earlySuccess = state.storage().put(key, value);

        replicas.remove(state.node());

        // Remove unavailable replicas + store hint
        var iter = replicas.iterator();
        while (iter.hasNext()) {
            Node replica = iter.next();

            if (replica.getStatus() != UP) {
                iter.remove();
                state.hintStorage().storeHint(replica.getId(), key, value);
            }
        }

        // Submit PUT to all replicas concurrently
        CompletionService<ZMsg> completionService =
                new ExecutorCompletionService<>(backupPool);

        for (Node node : replicas)
            completionService.submit(() -> {
                var reply = RequestUtils
                        .makeRPutRequest(getSocket(), node, key, value);

                if (reply == null && node.getStatus() == UP) {
                    // Timed out, mark as down + store hint
                    synchronized (state.ring()) {
                        node.markDown(state.node().getId());
                    }
                    state.hintStorage().storeHint(node.getId(), key, value);
                }
                return reply;
            });

        // Own node already succeeded, stop here
        if (earlySuccess)
            return ReplyUtils.success();

        if (replicas.isEmpty())
            return ReplyUtils.failure();

        long deadline = System.currentTimeMillis() + RequestHandler.TIMEOUT;

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
        } catch (InterruptedException | ExecutionException _) {
            //todo: Log exception?
            return ReplyUtils.failure();
        }

        // No successful responses
        return ReplyUtils.failure();
    }
}
