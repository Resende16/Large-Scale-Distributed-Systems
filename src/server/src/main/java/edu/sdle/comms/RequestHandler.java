package edu.sdle.comms;

import edu.sdle.*;
import edu.sdle.comms.requests.*;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.*;

public class RequestHandler {
    public static final int TIMEOUT = 1500; // per request

    private final NodeState state;
    private final Map<RequestType, Request> requests;

    public RequestHandler(NodeState state) {
        this.state = state;
        requests = new EnumMap<>(RequestType.class);
    }

    /**
     * Handle a client request.
     * @param msg Request to be handled
     * @return The reply as a ZMsg
     */
    public ZMsg handleRequest(ZMsg msg) {
        var typeStr = msg.pop().toString();
        var type = RequestType.parse(typeStr);

        if (type == null)
            // Invalid type
            return ReplyUtils.failure();

        var request = requests.get(type);
        if (request == null)
            return ReplyUtils.failure();

        // Dispatch to designated handler
        return request.handle(state, msg);
    }

    /**
     * Registers request handlers for various request types.
     * @param context ZMQ context used for creating the utility socket
     * @param backupPool Thread pool for sending requests to other nodes
     */
    public void registerRequests(ZContext context, ExecutorService backupPool) {
        // Create thread-local socket for node requests
        var socket = ThreadLocal.withInitial(() -> {
            ZMQ.Socket _socket = context.createSocket(SocketType.REQ);
            _socket.setReqRelaxed(true);
            _socket.setLinger(0);

            var R = state.ring().getR();

            // Receive timeout is set such that a GET can
            // succeed even if R-1 replicas are down
            _socket.setReceiveTimeOut(TIMEOUT / R);
            _socket.setSendTimeOut(300);
            return _socket;
        });

        requests.put(RequestType.GET, new GetRequest(socket));
        requests.put(RequestType.PUT, new PutRequest(socket, backupPool));
        requests.put(RequestType.REPLICA_GET, new RGetRequest());
        requests.put(RequestType.REPLICA_PUT, new RPutRequest());
        requests.put(RequestType.STATE, new StateRequest());
        requests.put(RequestType.GOSSIP, new GossipRequest());
    }
}
