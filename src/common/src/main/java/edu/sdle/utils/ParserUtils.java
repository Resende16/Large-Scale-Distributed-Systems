package edu.sdle.utils;

import java.util.UUID;

public class ParserUtils {
    // Parse a UUID object from a user-provided String,
    // returns null if failed
    public static UUID parseUUID(String argName, String arg) {
        if (arg == null)
            return null;

        try {
            return UUID.fromString(arg);
        } catch (IllegalArgumentException _) {
            System.err.println("Invalid " + argName + " '" + arg + "', not a UUID!");
            return null;
        }
    }

    // Parse a positive integer (>0), returns 0 if failed
    public static int parsePInt(String argName, String arg) {
        if (arg == null)
            return 0;

        try {
            var uint = Integer.parseInt(arg);
            if (uint < 1) {
                System.err.println("Invalid " + argName + " count (" + arg + "), must be at least 1!");
                return 0;
            }
            return uint;

        } catch (NumberFormatException _) {
            System.err.println("Invalid " + argName + " count  (" + arg + ")!");
            return 0;
        }
    }

    // Parse a zmq address, return true if the address is valid
    public static boolean parseZmqAddress(String arg) {
        if (arg == null)
            return false;

        if (arg.startsWith("tcp://")) {
            var parts = arg.substring(6).split(":");
            if (parts.length != 2) {
                System.err.println("Invalid address '" + arg + "', missing colon!");
                return false;
            }

            try {
                int port = Integer.parseInt(parts[1]);
                if (port < 1 || port > 65535) {
                    System.err.println("Invalid address '" + arg + "', port is out of range!");
                    return false;
                }
            } catch (NumberFormatException _) {
                System.err.println("Invalid address '" + arg + "', port is not a number!");
                return false;
            }
            return true;
        }

        if (arg.startsWith("ipc://") && arg.length() > 6) {
            return true;
        }

        System.err.println("Invalid address '" + arg + "', not a zmq address!");
        System.err.println("It must begin with tcp:// or ipc://.");
        return false;
    }
}
