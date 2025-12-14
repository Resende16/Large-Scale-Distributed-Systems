package edu.sdle;

import edu.sdle.comms.CommsManager;
import edu.sdle.comms.Gossiper;
import edu.sdle.comms.RequestHandler;
import edu.sdle.storage.FileStorage;
import edu.sdle.utils.RingUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Server {
    static void main(String[] args) {
        // Parse user args
        ArgParser parser = new ArgParser();
        if (!parser.parseArgs(args))
            return;

        // Create node state
        var node = new Node(parser.id, parser.address);
        var storage = new FileStorage("data/" + parser.id);
        var hintStorage = new HintStorage("data/" + parser.id + "/hints");

        var ring = Bootstrapper.bootstrap(parser.address, parser.vnodes, parser.R);
        if (ring == null)
            return; // Failed to bootstrap

        // Add self to ring
        ring.addNode(node, node.getId());

        var state = new NodeState(ring, storage, hintStorage, node);

        // Print node parameters
        parser.printArgs();

        var manager = new CommsManager(parser.workerThreads, parser.backupThreads);
        var handler = new RequestHandler(state);

        handler.registerRequests(manager.getContext(), manager.getBackupPool());
        manager.init(parser.address, handler);

        // Start gossiper
        var gossiper = new Gossiper(manager.getContext(), state, parser.address);
        gossiper.start();

        // Main loop
        loop(state, manager);

        gossiper.shutdown();
        manager.shutdown();
    }

    private static void loop(NodeState state, CommsManager manager) {
        //System.out.println("Enter LEAVE to leave the ring and exit");
        System.out.println("Enter EXIT to exit\n");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String input;

            while (true) {
                input = reader.readLine();

                // Stop the node process
                if ("EXIT".equals(input)) {
                    System.out.println("\nExiting...");
                    break;
                }

                // Simulate node being down
                if ("FREEZE".equals(input)) {
                    System.out.println("Freezing for 15 seconds!");
                    manager.freeze();
                }

                // Leave the ring + exit
                if ("LEAVE".equals(input)) {

                    //todo: stop manager and gossip first!

                    System.out.println("\nLeaving ring and exiting...");

                    var oldRing = state.ring().copy();

                    synchronized (state.ring()) {
                        state.ring().removeNode(state.node().getId());
                    }

                    // Wait for gossip to propagate
                    Thread.sleep(3000);

                    // Transfer out keys
                    RingUtils.transferKeys(oldRing,
                            state.ring(),
                            state.storage(),
                            state.node()
                    );
                    break;
                }
            }
        } catch (IOException | InterruptedException _) {}
    }
}
