package edu.sdle;

import java.util.Objects;
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

    public Node(UUID id, String zmqAddress) {
        this.id = id;

        var addr = zmqAddress.replace("tcp://", "");

        // Parse network address
        ip = addr.split(":")[0];
        port = Integer.parseInt(addr.split(":")[1]);
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

    @Override
    public String toString() {
        return "Node(" + id + ", " + ip + ":" + port + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Node node = (Node) obj;
        return id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
