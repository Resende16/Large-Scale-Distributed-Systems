package edu.sdle.comms.requests;

import edu.sdle.NodeState;
import org.zeromq.ZMsg;

public abstract class Request {
    // Handle a message and return a ZMsg reply
    public abstract ZMsg handle(NodeState state, ZMsg msg);
}
