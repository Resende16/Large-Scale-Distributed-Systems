package edu.sdle.comms.comms;

import edu.sdle.ProxyRegistry;
import edu.sdle.comms.ReplyUtils;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.utils.JsonUtils;
import edu.sdle.utils.UUIDUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.UUID;

public class CommsManager {
    private final ZMQ.Socket socket;

    public CommsManager() {
        try (ZContext context = new ZContext()) {
            socket = context.createSocket(SocketType.REQ);
        }
        socket.setSendTimeOut(3000);
        socket.setReceiveTimeOut(30000); //?
        socket.setReqRelaxed(true);
        socket.setLinger(0);
    }

    /**
     * Send a client request to the "cloud",
     * specifically to one of the known proxies.
     * @return Reply from the proxy; if timed out, FAILURE is returned
     */
    private ZMsg sendToCloud(ZMsg msg) {
        for (var node : ProxyRegistry.getInstance().getProxyAddresses()) {
            socket.connect(node);
            msg.send(socket);

            var reply = ZMsg.recvMsg(socket);
            socket.disconnect(node);

            if (reply != null)
                return reply;
            // Retry if proxy timed out
        }

        // All proxies timed out
        return ReplyUtils.failure();
    }

    /**
     * Get a Shopping List from the cloud
     * @param key Key of the Shopping List
     * @return A ShoppingList object, or null on fail
     */
    public ShoppingList getObj(UUID key) {
        String json = get(key);
        return JsonUtils.fromJson(json, ShoppingList.class);
    }

    /**
     * Get a Shopping List from the cloud, as a json string
     * @param key Key of the Shopping List
     * @return Json string on success, null otherwise
     */
    public String get(UUID key) {
        ZMsg msg = new ZMsg();
        msg.add(RequestType.GET.name());
        msg.add(UUIDUtils.uuidToBytes(key));

        var reply = sendToCloud(msg);

        if (!ReplyUtils.didSucceed(reply))
            return null;

        var valueFrame = msg.pop();
        if (valueFrame == null)
            return null; // malformed reply?

        return valueFrame.toString();
    }

    /**
     * "Put"/ upload a Shopping List to the cloud
     * @param shoppingList List to upload
     * @return Whether the operation was successful
     */
    public boolean put(ShoppingList shoppingList) {
        var value = JsonUtils.toJson(shoppingList);
        if (value == null)
            return false;

        ZMsg msg = new ZMsg();
        msg.add(RequestType.PUT.name());

        // Append key + value
        var keyBytes = UUIDUtils.uuidToBytes(shoppingList.getId());
        msg.add(keyBytes);
        msg.add(value);

        var reply = sendToCloud(msg);
        return ReplyUtils.didSucceed(reply);
    }
}
