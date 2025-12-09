package edu.sdle;

import java.util.List;

public class Proxy {

    private static String address = null;
    private static int workerThreads = 4;
    private static int backupThreads = 4;

    // Needed only when creating a new hash ring
    private static int timeout = 3000;
    private static int N = 2;

    static void main(String[] args) {
        var proxies = ProxyRegistry.getInstance().getProxyAddresses();

        // Parse user-provided args
        if (!parseArgs(args, proxies))
            return;

        // Print proxy parameters
        System.out.println("--------------------------");
        System.out.println("Proxy parameters:");
        System.out.println("Address: " + address);
        System.out.println("Worker threads: " + workerThreads);
        System.out.println("Backup threads: " + backupThreads);
        System.out.println("--------------------------");

        //todo
    }

    private static void printArgList() {
        System.out.println("-".repeat(50));
        System.out.printf("%-25s %s%n", "--address <ip:port>", "Address of the proxy");
        System.out.printf("%-25s %s%n", "--worker-threads <int>", "Number of worker threads");
        System.out.printf("%-25s %s%n", "--backup-threads <int>", "Number of backup threads");
        System.out.printf("%-25s %s%n", "--timeout <ms>", "Maximum time to process a client request");
        System.out.printf("%-25s %s%n", "--vnodes <int>", "Number of virtual nodes per physical node");
        System.out.printf("%-25s %s%n", "--N <int>", "Replication factor for each data item");
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

                    // Unknown proxy address
                    if (!proxies.contains(address)) {
                        System.err.println("This address is not a known proxy address!");
                        System.err.println("Use one of the following:");
                        for (var p : proxies) {
                            System.err.println(p);
                        }
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
                case "--backup-threads" -> {
                    try {
                        backupThreads = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid backup thread count (" + args[i] + ")!");
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