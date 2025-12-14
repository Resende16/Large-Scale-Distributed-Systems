package edu.sdle.comms.requests;

import edu.sdle.Node;
import edu.sdle.NodeState;
import edu.sdle.comms.ReplyUtils;
import edu.sdle.utils.RequestUtils;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.List;

public class GetRequest extends Request {
    private final ThreadLocal<ZMQ.Socket> socket;

    public GetRequest(ThreadLocal<ZMQ.Socket> socket) {
        this.socket = socket;
    }

    // Socket used for sending REPLICA_GET to replicas
    public ZMQ.Socket getSocket() {
        return socket.get();
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
        var value = state.storage().get(key);
        if (value != null)
            return ReplyUtils.success(value);

        // Not in local storage, try other nodes
        List<Node> replicas = state.ring().getNNodes(key);
        replicas.remove(state.node()); // remove self

        if (replicas.isEmpty())
            return ReplyUtils.failure();

        // Wait for the first successful response
        for (Node node : replicas) {
            var reply = RequestUtils.makeRGetRequest(getSocket(), node, key);

            if (ReplyUtils.didSucceed(reply) && !reply.isEmpty()) {
                var payload = reply.pop();
                return ReplyUtils.success(payload);
            }
        }

        // No successful reply
        return ReplyUtils.failure();
    }
}
