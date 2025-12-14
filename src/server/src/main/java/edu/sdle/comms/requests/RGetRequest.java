package edu.sdle.comms.requests;

import edu.sdle.NodeState;
import edu.sdle.comms.ReplyUtils;
import edu.sdle.utils.RequestUtils;
import org.zeromq.ZMsg;

public class RGetRequest extends Request {
    @Override
    public ZMsg handle(NodeState state, ZMsg msg) {
        // Parse key
        var key = RequestUtils.parseUUID(msg);
        if (key == null)
            return ReplyUtils.failure();

        //todo: Better logging
        System.out.println("> REPLICA_GET " + key);

        // Check local storage
        var value = state.storage().get(key);
        if (value != null)
            return ReplyUtils.success(value);
        else
            return ReplyUtils.failure();
    }
}
