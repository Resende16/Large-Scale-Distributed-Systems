package edu.sdle.utils;

import edu.sdle.Node;
import edu.sdle.comms.RequestType;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.UUID;

public class RequestUtils {

    // Parse a UUID from a ZMsg
    public static UUID parseUUID(ZMsg msg) {
        if (msg.isEmpty())
            return null;

        var bytes = msg.pop().getData();
        if (bytes.length != 16)
            // Invalid UUID
            return null;

        return UUIDUtils.uuidFromBytes(bytes);
    }

    // Send a ZMQ message to a Node
    public static ZMsg send(ZMQ.Socket socket, Node node, ZMsg msg) {
        var zmqAddr = node.getZMQAddress();
        socket.connect(zmqAddr);

        msg.send(socket);
        var reply = ZMsg.recvMsg(socket);

        socket.disconnect(zmqAddr);
        return reply;
    }

    // Make a REPLICA_GET request to a Node
    public static ZMsg makeRGetRequest(ZMQ.Socket socket, Node node, UUID key) {
        //System.out.println("< REPLICA_GET " + key + " to " + node.getZMQAddress());

        ZMsg msg = new ZMsg();
        msg.add(RequestType.REPLICA_GET.name());
        msg.add(UUIDUtils.uuidToBytes(key));

        return send(socket, node, msg);
    }

    // Make a REPLICA_PUT request to a Node
    public static ZMsg makeRPutRequest(ZMQ.Socket socket, Node node, UUID key, String value) {
        //System.out.println("< REPLICA_PUT " + key + " to " + node.getZMQAddress());

        ZMsg msg = new ZMsg();
        msg.add(RequestType.REPLICA_PUT.name());
        msg.add(UUIDUtils.uuidToBytes(key));
        msg.add(value);

        return send(socket, node, msg);
    }
}
