package edu.sdle.comms.requests;

import edu.sdle.NodeState;
import edu.sdle.comms.ReplyUtils;
import edu.sdle.utils.RequestUtils;
import org.zeromq.ZMsg;

public class RPutRequest extends Request {
    @Override
    public ZMsg handle(NodeState state, ZMsg msg) {
        // Parse key and value
        var key = RequestUtils.parseUUID(msg);
        var valueFrame = msg.pop();

        if (key == null || valueFrame == null)
            return ReplyUtils.failure();

        var value = valueFrame.toString();

        //todo: Better logging
        System.out.println("> REPLICA_PUT " + key);

        var replicas = state.ring().getNNodes(key);

        // Replica not responsible for the key
        if (!replicas.contains(state.node()))
            return ReplyUtils.failure();

        // Store locally
        if (state.storage().put(key, value))
            return ReplyUtils.success();
        else
            return ReplyUtils.failure();
    }
}
