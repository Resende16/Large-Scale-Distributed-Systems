package edu.sdle.comms;

import edu.sdle.NodeState;
import edu.sdle.hashring.Node;
import edu.sdle.utils.JsonUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static edu.sdle.hashring.NodeStatus.*;

/// Periodic, anti-entropy "Gossiper"
public class Gossiper {
    private final Random random;
    private final ZMQ.Socket socket;
    private final ScheduledExecutorService scheduler;

    private final NodeState state;

    public Gossiper(ZContext context, NodeState state) {
        this.state = state;
        this.random = new Random();

        this.socket = context.createSocket(SocketType.REQ);
        this.socket.setSendTimeOut(500);
        this.socket.setReceiveTimeOut(500);
        this.socket.setReqRelaxed(true);
        this.socket.setLinger(0);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Gossiper");
            t.setDaemon(true);
            return t;
        });
    }

    private Node selectRandomPeer() {
        var peers = state.ring().getNodes();

        // Remove self + filter for healthy (UP) nodes only
        peers.removeIf(node ->
                node.getId().equals(state.node().getId()) ||
                        node.getStatus() != UP
        );

        if (peers.isEmpty())
            return null;

        return peers.get(random.nextInt(peers.size()));
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::gossip,
                2, 1, TimeUnit.SECONDS
        );
    }

    public void shutdown() {
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                System.err.println("Gossiper did not terminate in time!");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }

        socket.close();
    }

    private void gossip() {
        try {
            var peer = selectRandomPeer();
            if (peer == null)
                return; // No more nodes in the ring

            String ringJson = JsonUtils.toJson(state.ring());
            if (ringJson == null) {
                System.err.println("Gossip round failed: serialized state is null");
                return;
            }

            // Mark self as UP in case we went down temporarily
            // and were marked as SUSPECT by other nodes
            synchronized (state.ring()) {
                state.node().keepAlive(state.node().getId());
            }

            var address = peer.getZmqAddress();

            socket.connect(address);
            socket.sendMore(RequestType.GOSSIP.name());
            socket.send(ringJson);

            var reply = socket.recvStr();

            // If timed out, mark as sus
            if (reply == null)
                peer.markSuspected(state.node().getId());

            socket.disconnect(address);

        } catch (Exception e) {
            System.err.println("Gossip round failed: " + e.getMessage());
        }
    }
}
