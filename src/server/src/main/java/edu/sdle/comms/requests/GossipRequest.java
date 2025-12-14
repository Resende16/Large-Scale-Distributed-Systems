package edu.sdle.comms.requests;

import edu.sdle.NodeState;
import edu.sdle.comms.ReplyUtils;
import edu.sdle.hashring.DistributedHashRing;
import edu.sdle.utils.JsonUtils;
import org.zeromq.ZMsg;

public class GossipRequest extends Request {
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

        //System.out.println("> GOSSIP");

        // Merge rings
        synchronized (state.ring()) {
            state.ring().merge(ring);
        }

        return ReplyUtils.success();
    }
}
