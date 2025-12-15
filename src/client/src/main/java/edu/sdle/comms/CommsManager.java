package edu.sdle.comms;

import edu.sdle.ProxyRegistry;
import edu.sdle.User;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.utils.JsonUtils;
import edu.sdle.utils.UUIDUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static edu.sdle.comms.ReplyCode.FAILURE_TIMEOUT;

public class CommsManager {
    private final ZContext context;
    private final ZMQ.Socket socket;

    // Reused for shuffling to avoid overloading the same proxy
    private final ArrayList<String> proxies =
            new ArrayList<>(ProxyRegistry.getInstance().getProxyAddresses());

    static void main() {
        var comms = new CommsManager(8000);
        var list = new ShoppingList("groceries", new User("vasco"));
        System.out.println(comms.put(list));
    }

    public CommsManager(int timeoutPerProxy) {
        context = new ZContext();
        socket = context.createSocket(SocketType.REQ);

        socket.setSendTimeOut(1000);
        socket.setReceiveTimeOut(timeoutPerProxy);
        socket.setReqRelaxed(true);
        socket.setLinger(0);
    }

    // Shutdown the communications manager
    public void shutdown() {
        socket.close();
        context.destroy();
    }

    /**
     * Send a client request to the "cloud",
     * specifically to one of the known proxies.
     * @return Reply from the proxy, or FAILURE_TIMEOUT
     */
    private ZMsg sendToCloud(ZMsg msg) {
        Collections.shuffle(proxies);

        for (var proxy : proxies) {
            socket.connect(proxy);
            msg.duplicate().send(socket);

            var reply = ZMsg.recvMsg(socket);

            socket.disconnect(proxy);

            if (reply != null)
                return reply;
            // Retry if proxy timed out
            //todo: check if busy too
        }

        // All proxies timed out
        return ReplyUtils.failure(FAILURE_TIMEOUT);
    }

    /**
     * Get a Shopping List from the cloud
     * @param key Key of the Shopping List
     * @return A ShoppingList object, or null on fail
     */
    public ShoppingList getObj(UUID key) {
        String json = get(key);
        if (json == null)
            return null;

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

        var valueFrame = reply.pop();
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
