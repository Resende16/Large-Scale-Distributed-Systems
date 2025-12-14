package edu.sdle.utils;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ZmqUtils {
    // Check if a network address is already in use
    public static boolean isAddressInUse(String address) {
        try (ZContext context = new ZContext();
             ZMQ.Socket socket = context.createSocket(SocketType.REQ)
        ) {
            address = address
                    .replace("localhost:", "*:")
                    .replace("127.0.0.1:", "*:");
            socket.bind(address);
            socket.unbind(address);
            return false;

        } catch (org.zeromq.ZMQException e) {
            // bingo
            return e.getErrorCode() == 48;

        }
    }
}
