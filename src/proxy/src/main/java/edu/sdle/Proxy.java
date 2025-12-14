package edu.sdle;

import edu.sdle.comms.CommsManager;
import edu.sdle.comms.RequestHandler;

public class Proxy {
    static void main(String[] args) {
        // Parse user args
        ArgParser parser = new ArgParser();
        if (!parser.parseArgs(args))
            return;

        parser.printArgs();

        var manager = new CommsManager(parser.workerThreads);
        var handler = new RequestHandler(manager.getContext());
        manager.init(parser.address, handler);

        // Exit when user presses enter key
        System.out.println("Press ENTER to exit\n");

        try {
            System.in.read();
        } catch (Exception _) {}

        System.out.println("Exiting...");

        manager.shutdown();
    }
}
