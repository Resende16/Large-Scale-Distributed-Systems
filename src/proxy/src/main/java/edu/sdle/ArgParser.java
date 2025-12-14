package edu.sdle;

import edu.sdle.utils.ZmqUtils;

public class ArgParser {

    // User-provided arguments
    public String address = null;
    public int workerThreads = 10;

    // Parse user-provided args
    public boolean parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--help" -> {
                    printArgList();
                    return false;
                }

                case "--address" -> {
                    if (++i > args.length)
                        continue;
                    address = args[i];

                    // Invalid address
                    if (!address.contains(":")) {
                        System.err.println("Invalid address '" + address + "', missing colon!");
                        return false;
                    }

                    // Unknown proxy address
                    var proxies = ProxyRegistry.getInstance().getProxyAddresses();
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
                    if (++i > args.length)
                        continue;

                    try {
                        workerThreads = Integer.parseInt(args[i]);
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

        // Address in use
        if (ZmqUtils.isAddressInUse(address)) {
            System.err.println("A proxy with the same address is already up and running!");
            return false;
        }

        return true;
    }

    // Print provided arguments post parsing
    public void printArgs() {
        System.out.print("\n- Proxy parameters");
        System.out.println("-".repeat(25));
        System.out.printf("%-20s %s%n", "Address", address);
        System.out.printf("%-20s %s%n", "Worker Threads", workerThreads);
        System.out.println("-".repeat(43));
        System.out.println();
    }

    // Print the list of arguments
    private void printArgList() {
        System.out.print("\n- List of arguments");
        System.out.println("-".repeat(30));
        System.out.printf("%-24s %s%n", "--address <ip:port>", "Address of the proxy");
        System.out.printf("%-24s %s%n", "--worker-threads <int>", "Number of worker threads");
        System.out.println("-".repeat(47));
    }
}