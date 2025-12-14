package edu.sdle.comms.requests;

import edu.sdle.Node;
import edu.sdle.UUIDUtils;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.UUID;

public class RequestUtils {

    public static UUID parseUUID(ZMsg msg) {
        if (msg.isEmpty())
            return null;

        var bytes = msg.pop().getData();
        if (bytes.length != 16)
            // Invalid UUID
            return null;

        return UUIDUtils.uuidFromBytes(bytes);
    }


    public static ZMsg makeRequest(ZMQ.Socket socket, Node node, ZMsg msg) {
        var zmqAddr = node.getZMQAddress();
        socket.connect(zmqAddr);
        msg.send(socket);

        var reply = ZMsg.recvMsg(socket);
        socket.disconnect(zmqAddr);
        return reply;
    }

    public static ZMsg makeGetRequest(ZMQ.Socket socket, Node node, UUID key) {
        System.out.println("< GET " + key + " to " + node.getZMQAddress());

        ZMsg msg = new ZMsg();
        msg.add(RequestType.GET.toString());
        msg.add(UUIDUtils.uuidToBytes(key));

        return makeRequest(socket, node, msg);
    }

    public static ZMsg makePutRequest(ZMQ.Socket socket, Node node, UUID key, String value) {
        System.out.println("< PUT " + key + " to " + node.getZMQAddress());

        ZMsg msg = new ZMsg();
        msg.add(RequestType.PUT.toString());
        msg.add(UUIDUtils.uuidToBytes(key));
        msg.add(value);

        return makeRequest(socket, node, msg);
    }
}
