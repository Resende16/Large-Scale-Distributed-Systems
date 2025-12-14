package edu.sdle;

import edu.sdle.comms.CommsManager;
import edu.sdle.comms.requests.RequestHandler;
import edu.sdle.hashring.DistributedHashRing;
import edu.sdle.storage.FileStorage;

public class Server {
    static void main(String[] args) {
        // Parse user args
        ArgParser parser = new ArgParser();
        if (!parser.parseArgs(args))
            return;


        // Create node state
        var node = new Node(parser.id, parser.address);
        var storage = new FileStorage("data/" + parser.id);
        //var ring = new DistributedHashRing(parser.vnodes, parser.R);

        var ring = Bootstrapper.bootstrap(parser.address, parser.vnodes, parser.R);
        if (ring == null)
            return;

        var state = new NodeState(ring, storage, node);

        parser.printArgs();

        var manager = new CommsManager(parser.workerThreads, parser.backupThreads);
        var handler = new RequestHandler(manager.getContext(), state, manager.getBackupPool(), storage);
        manager.init(parser.address, handler);

        //todo: Start gossiper

        // Exit when user presses enter key
        System.out.println("Press ENTER to exit\n");

        try {
            System.in.read();
        } catch (Exception _) {}

        System.out.println("Exiting...");

        manager.shutdown();
    }
}
