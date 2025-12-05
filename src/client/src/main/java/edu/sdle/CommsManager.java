/*
package edu.sdle;

import org.w3c.dom.Node;
import org.zeromq.ZMsg;
import zmq.ZMQ;

import java.util.UUID;

public class CommsManager {

    private final ZMQ.Socket socket;


    public ZMsg makeRequest(Node node, ZMsg msg) {
        var zmqAddr = node.getZMQAddress();
        socket.connect(zmqAddr);
        msg.send(socket);

        var reply = ZMsg.recvMsg(socket);
        socket.disconnect(zmqAddr);
        return reply;
    }

    public ZMsg makeGetRequest(Node node, UUID key) {
        ZMsg msg = new ZMsg();
        msg.add(RequestType.GET.toString());
        msg.add(UUIDUtils.uuidToBytes(key));
        return makeRequest(node, msg);
    }

    public ZMsg makePutRequest(Node node, UUID key, String value) {

        ZMsg msg = new ZMsg();
        msg.add(RequestType.PUT.toString());
        msg.add(UUIDUtils.uuidToBytes(key));
        msg.add(value);

        return makeRequest(node, msg);
    }

    public ZMsg handleGetRequest(UUID key) {

        long deadline = System.currentTimeMillis() + state.timeout();

        // Wait for the first successful response (sequential)
        for (String addr : ProxyRegistry.get) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0)
                break; // Timed out

            var reply = makeGetRequest(node, key);

            if (ReplyUtils.didSucceed(reply) && !reply.isEmpty()) {
                var payload = reply.pop();
                return ReplyUtils.success(payload);
            }
        }

        return ReplyUtils.failure();
    }
}
*/