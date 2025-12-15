package edu.sdle.comms.requests;

import edu.sdle.NodeState;
import edu.sdle.comms.ReplyUtils;
import edu.sdle.hashring.DistributedHashRing;
import edu.sdle.utils.JsonUtils;
import edu.sdle.utils.RingUtils;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

public class GossipRequest extends Request {
    private final ThreadLocal<ZMQ.Socket> socket;

    public GossipRequest(ThreadLocal<ZMQ.Socket> socket) {
        this.socket = socket;
    }

    // Socket used for data transfers when ring changes
    public ZMQ.Socket getSocket() {
        return socket.get();
    }

    @Override
    public ZMsg handle(NodeState state, ZMsg msg) {
        var frame = msg.pop();

        // Missing payload
        if (frame == null)
            return ReplyUtils.failure();

        var json = frame.toString();
        var ring = JsonUtils.fromJson(json, DistributedHashRing.class);

        // Invalid payload
        if (ring == null)
            return ReplyUtils.failure();

        // Merge rings
        var oldRing = state.ring().copy();

        synchronized (state.ring()) {
            state.ring().merge(ring);
        }

        for (var node: state.ring().getNodes()) {
            System.out.println(node.getZmqAddress() + " -> " + node.getStatus());
        }
        System.out.println();

        // Exchange data if necessary due to ring changes
        RingUtils.transferKeys(oldRing,
                state.ring(),
                state.storage(),
                getSocket(),
                state.node()
        );

        // Transfer hints too
        RingUtils.transferHints(oldRing,
                state.ring(),
                state.hintStorage(),
                getSocket()
        );

        return ReplyUtils.success();
    }
}
