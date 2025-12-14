package edu.sdle;

import edu.sdle.comms.ReplyUtils;
import edu.sdle.comms.RequestType;
import edu.sdle.hashring.DistributedHashRing;
import edu.sdle.storage.FileStorage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.List;
import java.util.UUID;

public class Bootstrapper {
    private final String ownAddress;

    private final int vnodes;
    private final int timeout;
    private final int N;

    public Bootstrapper(String ownAddress,
                        // To be used if creating its own state
                        int vnodes, int timeout, int N) {
        this.ownAddress = ownAddress;
        this.vnodes = vnodes;
        this.timeout = timeout;
        this.N = N;
    }

    public NodeState bootstrap() {
        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.REQ);
        socket.setSendTimeOut(700);
        socket.setReceiveTimeOut(700);
        socket.setReqRelaxed(true);
        socket.setLinger(0);

        if (isAddressInUse(socket)) {
            socket.close();
            context.close();
            return null;
        }

        List<String> proxies = ProxyRegistry.getInstance().getProxyAddresses();

        System.out.println("Starting bootstrap process...");

        // Try to contact proxies up to 3 times
        for (String addr : proxies) {
            if (addr.equals("tcp://" + ownAddress)) // Skip self
                continue;

            var state = requestState(socket, addr);

            if (state != null) {
                socket.close();
                context.destroy();
                System.out.println("Successfully bootstrapped from " + addr + "!");
                return state;
            }
        }

        socket.close();
        context.destroy();

        var firstProxy = proxies.getFirst();

        if (!firstProxy.equals(ownAddress)) {
            // Not allowed to bootstrap ourselves
            System.err.println("Cannot bootstrap: no other proxies are up and this is not the starting proxy!");
            System.err.println("Please start the proxy @ " + firstProxy + " to get started.");
            return null;
        }

        // Create its own state
        else {
            System.out.println("Bootstrapped as the starting proxy with a brand new state!");
            var hashRing = new DistributedHashRing(vnodes, 2);
            return new NodeState(hashRing, timeout, new FileStorage(""),new Node(UUID.randomUUID(), ownAddress));
        }
    }

    /**
     * Make a STATE request to a proxy.
     * @param zmqAddress ZMQ address of the proxy
     * @return A ProxyState object on success, null otherwise
     */
    private NodeState requestState(ZMQ.Socket socket,
                                   String zmqAddress) {
        socket.connect(zmqAddress);
        socket.send(RequestType.STATE.toString());

        var reply = ZMsg.recvMsg(socket);
        socket.disconnect(zmqAddress);

        if (!ReplyUtils.didSucceed(reply) || reply.isEmpty()) {
            return null;
        }

        var json = reply.pop().toString();
        return JsonUtils.fromJson(json, NodeState.class);
    }

    /**
     * Check if the proxy address is already in use.
     * @param socket Socket to use during the test
     * @return True if the address is in use
     */
    private boolean isAddressInUse(ZMQ.Socket socket) {
        String bindAddr = ownAddress
                .replace("tcp://localhost:", "tcp://*:")
                .replace("tcp://127.0.0.1:", "tcp://*:");

        try {
            socket.bind(bindAddr);
            socket.unbind(bindAddr);
            return false;

        } catch (org.zeromq.ZMQException e) {
            if (e.getErrorCode() == 48) {
                System.err.println("A proxy with the same address is already up and running!");
                return true;
            }
            return false;
        }
    }
}
