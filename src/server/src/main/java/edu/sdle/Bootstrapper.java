package edu.sdle;

import edu.sdle.comms.ReplyUtils;
import edu.sdle.comms.RequestType;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.List;

public class Bootstrapper {
    private final Node node;

    public Bootstrapper(Node node) {
        this.node = node;
    }

    public boolean bootstrap() {
        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.REQ);
        socket.setSendTimeOut(700);
        socket.setReceiveTimeOut(700);
        socket.setReqRelaxed(true);
        socket.setLinger(0);

        if (isAddressInUse(socket)) {
            socket.close();
            context.close();
            return false;
        }

        List<String> proxies = ProxyRegistry.getInstance().getProxyAddresses();

        System.out.println("Contacting a proxy...");

        // Try to contact proxies up to 3 times
        for (String addr : proxies) {

            var success = makeJoinRequest(socket, addr);

            if (success) {
                socket.close();
                context.destroy();
                System.out.println("Join accepted by " + addr + "!");
                return true;
            }
        }

        socket.close();
        context.destroy();

        System.out.println("Failed to contact a proxy, are you sure they are running?");
        return false;
    }

    private boolean makeJoinRequest(ZMQ.Socket socket,
                                    String zmqAddress) {
        socket.connect(zmqAddress);
        socket.sendMore(RequestType.JOIN.toString());
        socket.sendMore(UUIDUtils.uuidToBytes(node.getId()));
        socket.send(node.getZMQAddress());

        var reply = ZMsg.recvMsg(socket);
        socket.disconnect(zmqAddress);

        return ReplyUtils.didSucceed(reply);
    }

    /**
     * Check if the proxy address is already in use.
     * @param socket Socket to use during the test
     * @return True if the address is in use
     */
    private boolean isAddressInUse(ZMQ.Socket socket) {
        String bindAddr = node.getZMQAddress()
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
