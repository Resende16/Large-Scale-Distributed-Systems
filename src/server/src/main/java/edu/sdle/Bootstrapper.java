package edu.sdle;

import edu.sdle.comms.ReplyUtils;
import edu.sdle.comms.RequestType;
import edu.sdle.hashring.DistributedHashRing;
import edu.sdle.utils.JsonUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.List;

public class Bootstrapper {

    public static DistributedHashRing bootstrap(String address, int vnodes, int R) {
        System.out.println("Starting bootstrap process...");

        // Create socket
        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.REQ);

        socket.setSendTimeOut(1000);
        socket.setReceiveTimeOut(750);
        socket.setReqRelaxed(true);
        socket.setLinger(0);

        // Get ring from seed nodes
        var seeds = SeedNodeRegistry.getInstance().getSeedAddresses();
        var ring = getRing(socket, seeds, address);

        // If that fails, try to create it
        if (ring == null)
            ring = newRing(seeds, address, vnodes, R);

        // Clean up
        socket.close();
        context.destroy();

        return ring;
    }

    /// Return a new ring, or null if node is not allowed to create it
    private static DistributedHashRing newRing(List<String> seeds, String address, int vnodes, int R) {
        var firstSeed = seeds.getFirst();

        if (firstSeed.equals(address)) {
            // Create ring
            System.out.println("Bootstrapped as the starting seed with a brand new ring!");
            return new DistributedHashRing(vnodes, R);
        }
        else {
            // Not allowed to create ring, fail bootstrapping
            System.err.println("Cannot bootstrap: no seed nodes are up and this is not the starting seed!");
            System.err.println("Please start the node @ " + firstSeed + " to get started.");
            return null;
        }
    }

    /// Get the hash ring by contacting each seed node until obtaining a response
    private static DistributedHashRing getRing(ZMQ.Socket socket, List<String> seeds, String address) {
        for (var seed : seeds) {
            if (seed.equals(address))
                continue;

            var ring = getRing(socket, seed);
            if (ring != null) {
                System.out.println("Successfully bootstrapped from " + seed + "!");
                return ring;
            }
        }

        // All seeds are down
        return null;
    }

    /// Get the hash ring from a seed node
    private static DistributedHashRing getRing(ZMQ.Socket socket, String address) {
        socket.connect(address);
        socket.send(RequestType.STATE.name());

        var reply = ZMsg.recvMsg(socket);
        socket.disconnect(address);

        if (!ReplyUtils.didSucceed(reply) || reply.isEmpty())
            return null;

        var json = reply.pop().toString();
        return JsonUtils.fromJson(json, DistributedHashRing.class);
    }
}
