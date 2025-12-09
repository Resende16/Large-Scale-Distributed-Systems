package edu.sdle;

import java.util.UUID;

public class Node {
    private final UUID id;
    private final String ip;
    private final int port;

    public Node(UUID id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public Node(UUID id, String addressString) {
        this.id = id;

        // Parse network address
        ip = addressString.split(":")[0];
        port = Integer.parseInt(addressString.split(":")[1]);
    }

    public UUID getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    // Get the Node's network address as a string
    public String getNetAddress() {
        return ip + ":" + port;
    }

    // Get the Node's zmq tcp address as a string
    public String getZMQAddress() {
        return "tcp://" + ip + ":" + port;
    }
}