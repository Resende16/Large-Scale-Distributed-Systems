package edu.sdle.comms.requests;

import edu.sdle.Node;
import edu.sdle.NodeState;
import edu.sdle.comms.ReplyUtils;
import org.zeromq.ZMsg;

import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class PutRequest extends Request {
    public PutRequest(RequestHandler handler) {
        super(handler);
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

        List<Node> replicas = state.ring().getNNodes(key);
        boolean earlySuccess = false;

        // Own node is responsible, put locally
        if (replicas.contains(state.node()))
            earlySuccess = state.storage().put(key, value);

        replicas.remove(state.node());

        // Submit PUT to all replicas concurrently
        CompletionService<ZMsg> completionService =
                new ExecutorCompletionService<>(handler.backupPool);

        for (Node node : replicas)
            completionService.submit(() ->
                    RequestUtils.makePutRequest(handler.socket.get(), node, key, value));

        // Own node already succeeded, stop here
        if (earlySuccess)
            return ReplyUtils.success();

        if (replicas.isEmpty())
            return ReplyUtils.failure();

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
}
