package edu.sdle;

import edu.sdle.comms.CommsManager;
import edu.sdle.comms.RequestHandler;
import edu.sdle.storage.FileStorage;

import java.util.List;
import java.util.UUID;

public class Server {

    private static String address = null;
    private static int workerThreads = 4;

    static void main(String[] args) {
        var proxies = ProxyRegistry.getInstance().getProxyAddresses();

        // Parse user-provided args
        if (!parseArgs(args, proxies))
            return;


        Node node = new Node(UUID.randomUUID(), address);

        Bootstrapper bs = new Bootstrapper(node);
        if (!bs.bootstrap())
            return;

        // Just in case
        String bindAddr = address
                .replace("tcp://localhost:", "tcp://*:")
                .replace("tcp://127.0.0.1:", "tcp://*:");

        // Print server parameters
        System.out.println();

        System.out.print("- Server parameters ");
        System.out.println("-".repeat(20));
        System.out.printf("%-15s %s%n", "Address", address);
        System.out.printf("%-15s %s%n", "Workers", workerThreads);
        System.out.println("-".repeat(40));

        System.out.println();

        var manager = new CommsManager(workerThreads);
        var storage = new FileStorage(address);
        var handler = new RequestHandler(storage);
        manager.init(bindAddr, handler);

        System.out.println("Press ENTER to exit");
        try {
            System.in.read();
        } catch (Exception e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
        System.out.println("Exiting...");

        manager.shutdown();
    }

    private static void printArgList() {
        System.out.println("-".repeat(50));
        System.out.printf("%-25s %s%n", "--address <ip:port>", "Address of the node");
        System.out.printf("%-25s %s%n", "--worker-threads <int>", "Number of worker threads");
        System.out.println("-".repeat(50));
    }

    private static boolean parseArgs(String[] args, List<String> proxies) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--help" -> {
                    printArgList();
                    return false;
                }
                case "--address" -> {
                    address = args[++i];

                    // Invalid address
                    if (!address.contains(":")) {
                        System.err.println("Invalid address '" + address + "', missing colon!");
                        return false;
                    }

                    // Reserved address
                    if (proxies.contains(address)) {
                        System.err.println("This address belongs to a known proxy and cannot be used by a server!");
                        return false;
                    }
                }
                case "--worker-threads" -> {
                    try {
                        workerThreads = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid worker thread count (" + args[i] + ")!");
                        return false;
                    }
                }
                default -> {
                    System.out.println("Unknown argument " + arg + " skipped!");
                    ++i;
                }
            }
        }

        // Address not provided
        if (address == null) {
            System.err.println("Address not specified, use --address!");
            System.err.println("You may use --help for a list of arguments.");
            return false;
        }

        return true;
    }
}
