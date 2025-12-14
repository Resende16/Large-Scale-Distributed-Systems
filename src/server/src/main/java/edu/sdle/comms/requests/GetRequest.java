package edu.sdle.comms.requests;

import edu.sdle.Node;
import edu.sdle.NodeState;
import edu.sdle.comms.ReplyUtils;
import org.zeromq.ZMsg;

import java.util.List;

public class GetRequest extends Request {
    public GetRequest(RequestHandler handler) {
        super(handler);
    }

    @Override
    public ZMsg handle(NodeState state, ZMsg msg) {
        // Parse key
        var key = RequestUtils.parseUUID(msg);
        if (key == null)
            return ReplyUtils.failure();

        //todo: Better logging
        System.out.println("> GET " + key);

        // Check local storage
        var value = handler.storage.get(key);
        if (value != null)
            return ReplyUtils.success(value);

        // Try elsewhere
        List<Node> replicas = state.ring().getNNodes(key);
        replicas.remove(state.node()); // remove self

        if (replicas.isEmpty())
            return ReplyUtils.failure();

        long deadline = System.currentTimeMillis() + state.timeout();

        // Wait for the first successful response
        for (Node node : replicas) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0)
                break; // Timed out

            var reply = RequestUtils.makeGetRequest(handler.socket.get(), node, key);

            if (ReplyUtils.didSucceed(reply) && !reply.isEmpty()) {
                var payload = reply.pop();
                return ReplyUtils.success(payload);
            }
        }

        // No successful reply
        return ReplyUtils.failure();
    }
}
