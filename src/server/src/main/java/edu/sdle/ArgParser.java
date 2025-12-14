package edu.sdle;

import edu.sdle.utils.ParserUtils;
import edu.sdle.utils.ZmqUtils;

import java.util.UUID;

public class ArgParser {

    // User-provided arguments
    // proxy params
    public UUID id = UUID.randomUUID();
    public String address = null;
    public int workerThreads = 10;
    public int backupThreads = 20;

    // ring params
    public int R = 2;
    public int vnodes = 50;

    // Parse user-provided args
    public boolean parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--help" -> {
                    printArgList();
                    return false;
                }

                case "--id" -> {
                    if (++i >= args.length)
                        break;

                    var _id = ParserUtils.parseUUID("node id", args[i]);
                    if (_id != null)
                        id = _id;
                    else return false; // exit if invalid
                }

                case "--address" -> {
                    if (++i >= args.length)
                        break;

                    if (!ParserUtils.parseZmqAddress(args[i]))
                        return false; // not a valid address

                    var proxies = ProxyRegistry.getInstance().getProxyAddresses();

                    if (proxies.contains(args[i])) { // reserved address
                        System.err.println("This address is reserved for a proxy!");
                        return false;
                    }

                    address = args[i];
                }

                // Numeric arguments

                case "--worker-threads" -> {
                    if (++i >= args.length)
                        break;

                    var val = ParserUtils.parsePInt("worker thread", args[i]);
                    if (val != 0)
                        workerThreads = val;
                    else return false;
                }

                case "--backup-threads" -> {
                    if (++i >= args.length)
                        break;

                    var val = ParserUtils.parsePInt("backup thread", args[i]);
                    if (val != 0)
                        backupThreads = val;
                    else return false;
                }

                case "--vnodes" -> {
                    if (++i >= args.length)
                        break;

                    var val = ParserUtils.parsePInt("virtual nodes", args[i]);
                    if (val != 0)
                        vnodes = val;
                    else return false;
                }

                case "--R" -> {
                    if (++i >= args.length)
                        break;

                    var val = ParserUtils.parsePInt("R", args[i]);
                    if (val != 0)
                        R = val;
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
        System.out.print("\n- Node parameters");
        System.out.println("-".repeat(25));
        System.out.println("UID   " + id);
        System.out.printf("%-20s %s%n", "Address", address);
        System.out.printf("%-20s %s%n", "Worker Threads", workerThreads);
        System.out.printf("%-20s %s%n", "Backup Threads", backupThreads);
        System.out.println("-".repeat(43));
        System.out.println();
    }

    // Print the list of arguments
    private void printArgList() {
        System.out.print("\n- List of arguments");
        System.out.println("-".repeat(38));
        System.out.printf("%-24s %s%n", "--id <uuid>", "Unique id of the node");
        System.out.printf("%-24s %s%n", "--address <string>", "Address of the node");
        System.out.printf("%-24s %s%n", "--worker-threads <int>", "Number of worker threads");
        System.out.printf("%-24s %s%n", "--backup-threads <int>", "Number of backup threads");
        System.out.printf("%-24s %s%n", "--vnodes <int>", "Virtual nodes per physical node");
        System.out.printf("%-24s %s%n", "--R <int>", "Replication factor of the ring");
        System.out.println("-".repeat(57));
    }
}