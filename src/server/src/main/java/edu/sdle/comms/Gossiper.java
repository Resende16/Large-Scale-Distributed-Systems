package edu.sdle.comms;

import edu.sdle.JsonUtils;
import edu.sdle.ProxyRegistry;
import edu.sdle.NodeState;
import edu.sdle.comms.requests.RequestType;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/// Periodic, anti-entropy "Gossiper"
public class Gossiper {
    private final List<String> peers;
    private final NodeState state;
    private final ZContext context;
    private final Random random;
    private final ZMQ.Socket socket;
    private final ScheduledExecutorService scheduler;

    public Gossiper(ZContext context, NodeState state, String ownAddress) {
        var proxies = ProxyRegistry.getInstance().getProxyAddresses();
        peers = new ArrayList<>(proxies);
        peers.remove(ownAddress);

        this.context = context;
        this.state = state;
        this.random = new Random();

        // Single PUSH socket, reused across rounds
        this.socket = context.createSocket(SocketType.REQ);
        this.socket.setSendTimeOut(1500);
        this.socket.setLinger(0);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Gossiper");
            t.setDaemon(true);
            return t;
        });
    }

    private String selectRandomPeer() {
        return peers.get(random.nextInt(peers.size()));
    }

    public void start() {
        if (peers.isEmpty())
            return;

        scheduler.scheduleAtFixedRate(
                this::gossip,
                3, 2, TimeUnit.SECONDS
        );
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
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
            String ringJson;

            synchronized (state) {
                ringJson = JsonUtils.toJson(state.ring());
            }

            if (ringJson == null) {
                System.err.println("Gossip round failed: serialized state is null");
                return;
            }

            System.out.println("gossiped");

            // Connect to peer
            socket.connect(peer);

            // Send gossip message
            socket.sendMore(RequestType.GOSSIP.name());
            socket.send(ringJson);


            var reply = socket.recvStr();

            // Disconnect after sending
            socket.disconnect(peer);

        } catch (Exception e) {
            System.err.println("Gossip round failed: " + e.getMessage());
        }
    }
}
