package edu.sdle.comms;

import edu.sdle.Node;
import edu.sdle.NodeState;
import edu.sdle.utils.JsonUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/// Periodic, anti-entropy "Gossiper"
public class Gossiper {
    private final Random random;
    private final ZMQ.Socket socket;
    private final ScheduledExecutorService scheduler;

    private final NodeState state;

    private ScheduledFuture<?> gossipTask;

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

    // For testing
    public void freeze() {
        // Cancel current gossip and start again in 15s
        gossipTask.cancel(true);
        scheduler.schedule(this::start, 15, TimeUnit.SECONDS);
    }

    private Node selectRandomPeer() {
        var peers = state.ring().getNodes();

        peers.remove(state.node()); // remove self

        if (peers.isEmpty())
            return null;

        return peers.get(random.nextInt(peers.size()));
    }

    public void start() {
        if (gossipTask != null && !gossipTask.isCancelled()) {
            gossipTask.cancel(false);
        }

        System.out.println("Starting gossiper!");

        gossipTask = scheduler.scheduleAtFixedRate(
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
                var self = state.ring().getRingNode(state.node().getId());
                if (self != null)
                    self.keepAlive(state.node().getId());
            }

            var address = peer.getZMQAddress();

            socket.connect(address);
            socket.sendMore(RequestType.GOSSIP.name());
            socket.send(ringJson);

            socket.recvStr();
            //todo: Detect failures based on reply

            socket.disconnect(address);

        } catch (Exception e) {
            System.err.println("Gossip round failed: " + e.getMessage());
        }
    }
}
