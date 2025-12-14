package edu.sdle.comms.requests;

import edu.sdle.NodeState;
import edu.sdle.comms.ReplyUtils;
import edu.sdle.utils.JsonUtils;
import org.zeromq.ZMsg;

public class StateRequest extends Request {
    @Override
    public ZMsg handle(NodeState state, ZMsg msg) {
        var ringJson = JsonUtils.toJson(state.ring());
        if (ringJson == null)
            return ReplyUtils.failure();

        //todo: Better logging
        System.out.println("> STATE");

        return ReplyUtils.success(ringJson);
    }
}
