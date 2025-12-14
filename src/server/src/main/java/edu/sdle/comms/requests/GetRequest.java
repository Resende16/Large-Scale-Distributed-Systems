package edu.sdle.comms.requests;

import edu.sdle.Node;
import edu.sdle.NodeState;
import edu.sdle.comms.ReplyUtils;
import edu.sdle.utils.RequestUtils;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import static edu.sdle.hashring.NodeStatus.*;

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
        var replicas = state.ring().getNNodes(key);
        replicas.remove(state.node()); // remove self

        if (replicas.isEmpty())
            return ReplyUtils.failure();

        //todo: return SUCCESS if stored hint

        // Wait for the first successful response
        for (Node node : replicas) {
            //todo: Migrate Node to RingNode
            var ringNode = state.ring().getRingNode(node.getId());
            if (ringNode.getStatus() != UP) {
                System.out.println("skipped node, MARKED as sus");
                synchronized (ringNode) { // ?
                    ringNode.markSuspected(state.node().getId());
                }
                // Skip it
                continue;
            }

            var reply = RequestUtils.makeRGetRequest(getSocket(), node, key);

            if (reply == null) {
                // timed out, mark as sus
                synchronized (ringNode) {
                    System.out.println("MARKING SUS " + node.getId());
                    ringNode.markSuspected(state.node().getId());
                }
            }

            if (ReplyUtils.didSucceed(reply) && !reply.isEmpty()) {
                var payload = reply.pop();
                return ReplyUtils.success(payload);
            }
        }

        // No successful reply
        return ReplyUtils.failure();
    }
}
