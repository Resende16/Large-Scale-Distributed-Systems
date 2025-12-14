package edu.sdle.comms;

import edu.sdle.NodeState;
import edu.sdle.utils.JsonUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/// Periodic, anti-entropy "Gossiper"
public class Gossiper {
    private final Random random;
    private final ZMQ.Socket socket;
    private final ScheduledExecutorService scheduler;

    private final NodeState state;
    private final String ownAddress;

    public Gossiper(ZContext context, NodeState state, String ownAddress) {
        this.state = state;
        this.ownAddress = ownAddress;
        this.random = new Random();

        this.socket = context.createSocket(SocketType.REQ);
        this.socket.setSendTimeOut(500);
        this.socket.setReceiveTimeOut(500);
        this.socket.setLinger(0);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Gossiper");
            t.setDaemon(true);
            return t;
        });
    }

    private String selectRandomPeer() {
        List<String> peers = new ArrayList<>();

        // Build list of peers
        for (var entry : state.ring().getMembers().entrySet()) {
            var addr = entry.value().value();

            if (!addr.equals(ownAddress))
                peers.add(entry.value().value());
        }

        if (peers.isEmpty())
            return null;

        return peers.get(random.nextInt(peers.size()));
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::gossip,
                3, 1, TimeUnit.SECONDS
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
            String peer = selectRandomPeer();
            if (peer == null)
                return; // No more nodes in the ring

            String ringJson = JsonUtils.toJson(state.ring());
            if (ringJson == null) {
                System.err.println("Gossip round failed: serialized state is null");
                return;
            }

            socket.connect(peer);
            socket.sendMore(RequestType.GOSSIP.name());
            socket.send(ringJson);

            socket.recvStr();
            //todo: Detect failures based on reply

            socket.disconnect(peer);

        } catch (Exception e) {
            System.err.println("Gossip round failed: " + e.getMessage());
        }
    }
}
