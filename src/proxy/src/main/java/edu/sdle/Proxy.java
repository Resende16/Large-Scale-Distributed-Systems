package edu.sdle;

import edu.sdle.comms.CommsManager;
import edu.sdle.comms.RequestHandler;

import java.util.List;

public class Proxy {

    private static String address = null;
    private static int workerThreads = 5;
    private static int backupThreads = 10;

    // Needed only when creating a new hash ring
    private static int vnodes = 2;
    private static int timeout = 3000;
    private static int N = 2;

    static void main(String[] args) {
        var proxies = ProxyRegistry.getInstance().getProxyAddresses();

        // Parse user-provided args
        if (!parseArgs(args, proxies))
            return;

        var bootstrapper = new Bootstrapper(address, vnodes, timeout, N);
        var proxyState = bootstrapper.bootstrap();

        if (proxyState == null)
            return;

        // Print proxy parameters
        System.out.println();
        System.out.print("- Proxy parameters");
        System.out.println("-".repeat(25));
        //System.out.println("\nSpecific to this proxy:");
        System.out.printf("%-20s %s%n", "Address", address);
        System.out.printf("%-20s %s%n", "Worker Threads", workerThreads);
        System.out.printf("%-20s %s%n", "Backup Threads", backupThreads);
        System.out.println("-".repeat(43));
        System.out.print("\n- Ring parameters");
        System.out.println("-".repeat(25));
        System.out.printf("%-25s %s%n", "Timeout", proxyState.timeout() + "ms");
        System.out.printf("%-25s %s%n", "Virtual Nodes",  proxyState.ring().getVnodesPerNode());
        System.out.printf("%-25s %s%n", "Replication Factor (N)", proxyState.N());
        System.out.println("-".repeat(43));
        System.out.println();

        // Just in case
        String bindAddr = address
                .replace("tcp://localhost:", "tcp://*:")
                .replace("tcp://127.0.0.1:", "tcp://*:");

        var manager = new CommsManager(workerThreads, backupThreads);
        var handler = new RequestHandler(manager.getContext(), proxyState, manager.getBackupPool());
        manager.init(bindAddr, handler);

        System.out.println("Press ENTER to exit");
        try {
            System.in.read();
        } catch (Exception e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
        System.out.println("Exiting...");

        manager.shutdown();

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
                        System.err.println("This address is not a known proxy!");
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
                case "--timeout" -> {
                    try {
                        timeout = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid timeout parameter (" + args[i] + ")!");
                        return false;
                    }
                }
                case "--N" -> {
                    try {
                        N = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid parameter N (" + args[i] + ")!");
                        return false;
                    }
                }
                case "--vnodes" -> {
                    try {
                        vnodes = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid parameter vnodes (" + args[i] + ")!");
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
