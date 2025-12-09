package edu.sdle;

import java.util.List;

public class Server {

    private static String address = null;
    private static int workerThreads = 4;

    static void main(String[] args) {
        var proxies = ProxyRegistry.getInstance().getProxyAddresses();

        // Parse user-provided args
        if (!parseArgs(args, proxies))
            return;

        // Print server parameters
        System.out.println("--------------------------");
        System.out.println("Server parameters:");
        System.out.println("Address: " + address);
        System.out.println("Worker threads: " + workerThreads);
        System.out.println("--------------------------");

        //todo
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