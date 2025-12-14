package edu.sdle.comms;

import edu.sdle.*;
import edu.sdle.utils.UUIDUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class RequestHandler {
    private final ThreadLocal<ZMQ.Socket> socket;
    private final List<String> seedNodes;

    public RequestHandler(ZContext context) {
        // Create thread-local socket to process
        // client requests and avoid locks

        socket = ThreadLocal.withInitial(() -> {
            ZMQ.Socket _socket = context.createSocket(SocketType.REQ);

            _socket.setReceiveTimeOut(5000);
            _socket.setSendTimeOut(2000);
            _socket.setReqRelaxed(true);
            _socket.setLinger(0);
            return _socket;
        });

        // Copy is kept to avoid recreating Array when shuffling,
        // since registry version is immutable
        seedNodes = new ArrayList<>(SeedNodeRegistry.getInstance().getSeedAddresses());
    }

    /**
     * Handle a client request.
     * @param msg Client request as a ZMsg
     * @return Reply provided by a node
     */
    public ZMsg handleRequest(ZMsg msg) {
        var msgCopy = msg.duplicate();

        // Check type
        var typeStr = msg.popString();
        var type = RequestType.parse(typeStr);
        if (type == null)
            return ReplyUtils.failure();

        // Validate before forwarding
        // check key first
        var key = parseKey(msg);
        if (key == null)
            return ReplyUtils.failure();

        // check value for PUT req
        if (type.equals(RequestType.PUT) && msg.pop() == null)
            return ReplyUtils.failure();

        // Log request
        //todo: Better logging
        System.out.println("> " + typeStr + " " + key);

        // Forward to a seed server
        return forward(msgCopy);
    }

    /**
     * Forward a client message to a seed node for processing.
     * @param msg ZMsg to be forwarded
     * @return Reply provided by the node
     */
    private ZMsg forward(ZMsg msg) {
        var socket = this.socket.get();

        // Shuffle to avoid overloading the same seed
        Collections.shuffle(seedNodes);

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

    /**
     * Parse a UUID key from a client request.
     * @param msg ZMsg to parse the key from
     * @return The parsed key as a UUID
     */
    public static UUID parseKey(ZMsg msg) {
        if (msg.isEmpty())
            return null;

        var bytes = msg.pop().getData();
        if (bytes.length != 16)
            // Invalid UUID
            return null;

        return UUIDUtils.uuidFromBytes(bytes);
    }
}
