package edu.sdle.comms.requests;

import edu.sdle.NodeState;
import org.zeromq.ZMsg;

public abstract class Request {

    final RequestHandler handler;
    final NodeState state;

    public Request(RequestHandler handler) {
        this.handler = handler;
        this.state = handler.state;
    }

    // Handle a message and return a ZMsg reply
    public abstract ZMsg handle(NodeState state, ZMsg msg);
}
