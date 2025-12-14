package edu.sdle;

import edu.sdle.utils.ParserUtils;
import edu.sdle.utils.ZmqUtils;

public class ArgParser {

    // User-provided arguments
    public String address = null;
    public int workerThreads = 30;

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
                    if (++i >= args.length)
                        break;

                    if (!ParserUtils.parseZmqAddress(args[i]))
                        return false; // not a valid address

                    var proxies = ProxyRegistry.getInstance().getProxyAddresses();

                    // Unknown proxy address
                    if (!proxies.contains(args[i])) {
                        System.err.println("This address is not a known proxy!");
                        System.err.println("Use one of the following:");

                        for (var p : proxies) {
                            System.err.println(p);
                        }
                        return false;
                    }

                    address = args[i];
                }

                case "--worker-threads" -> {
                    if (++i >= args.length)
                        break;

                    var val = ParserUtils.parsePInt("worker thread", args[i]);
                    if (val != 0)
                        workerThreads = val;
                    else return false;
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
            System.err.println("This address is currently in use!");
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
        System.out.printf("%-24s %s%n", "--address <string>", "Address of the proxy");
        System.out.printf("%-24s %s%n", "--worker-threads <int>", "Number of worker threads");
        System.out.println("-".repeat(47));
    }
}