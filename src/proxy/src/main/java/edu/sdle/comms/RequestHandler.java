package edu.sdle.comms;

import edu.sdle.*;
import edu.sdle.comms.requests.RequestType;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.Collections;
import java.util.concurrent.*;

public class RequestHandler {
    private final ThreadLocal<ZMQ.Socket> socket;

    public RequestHandler(ZContext context) {
        // Create thread-local socket to process
        // client requests and avoid locks

        socket = ThreadLocal.withInitial(() -> {
            ZMQ.Socket _socket = context.createSocket(SocketType.REQ);

            _socket.setReceiveTimeOut(10);
            _socket.setSendTimeOut(3);
            _socket.setReqRelaxed(true);
            _socket.setLinger(0);
            return _socket;
        });
    }

    /**
     * Handle a client request.
     * @param msg Client request as a ZMsg
     * @return Reply provided by a node
     */
    private ZMsg handle(ZMsg msg) {
        var msgCopy = msg.duplicate();

        // Check type
        var typeStr = msg.popString();
        var type = RequestType.parse(typeStr);

        if (type == null)
            return ReplyUtils.failure();

        // Forward to a seed server
        return forward(msgCopy);
    }

    /**
     * Forward a client message to a seed node for processing.
     * @param msg ZMsg to be forwarded
     * @return Reply provided by the node
     */
    private ZMsg forward(ZMsg msg) {
        var seedNodes = SeedNodeRegistry.getInstance().getSeedAddresses();

        // Shuffle to avoid overloading the same seed server
        Collections.shuffle(seedNodes);

        var socket = this.socket.get();

        for (var node : seedNodes) {
            socket.connect(node);

            // Forward + wait for reply
            msg.send(socket);

            var reply = ZMsg.recvMsg(socket);
            socket.disconnect(node);

            if (reply != null)
                return reply;
        }

        // Timed out
        return ReplyUtils.failure();
    }
}
