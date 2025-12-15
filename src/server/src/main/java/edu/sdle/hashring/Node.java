package edu.sdle.hashring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sdle.crdt.ICRDT;
import edu.sdle.crdt.lww.LWWRegister;

import java.util.Objects;
import java.util.UUID;

import static edu.sdle.hashring.NodeStatus.*;

public class Node implements ICRDT<Node> {
    @JsonProperty
    private UUID id;

    // Node's zmq network address stored as a LWWRegister to
    // allow for a node to dynamically change its net address
    // without leaving the ring
    @JsonProperty
    private LWWRegister<String> address;

    @JsonProperty
    private final LWWRegister<NodeStatus> status;

    public Node(UUID nodeId, String nodeAddr) {
        id = nodeId;
        this.address = new LWWRegister<>(nodeAddr, nodeId);
        this.status = new LWWRegister<>(UP, nodeId);
    }

    // For deserialization
    @JsonCreator
    private Node(
            @JsonProperty("address") LWWRegister<String> address,
            @JsonProperty("status") LWWRegister<NodeStatus> status
    ) {
        this.address = address;
        this.status = status;
    }

    @JsonIgnore
    public UUID getId() {
        return id;
    }

    @JsonIgnore
    public String getZmqAddress() {
        return address.value();
    }

    @JsonIgnore
    public NodeStatus getStatus() {
        return status.value();
    }

    public void updateAddress(String newAddr, UUID issuerNode) {
        address.assign(newAddr, issuerNode);
    }

    // same as marking UP
    public void keepAlive(UUID issuerNode) {
        status.assign(UP, issuerNode);
    }

    public void markDown(UUID issuerNode) {
        if (status.value() == UP)
            status.assign(DOWN, issuerNode);
    }

    @Override
    public void merge(Node other) {
        address.merge(other.address);
        status.merge(other.status);
    }

    @Override
    public String toString() {
        return "RingNode{" +
                "address=" + address.value() +
                ", status=" + status.value() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Node node = (Node) o;
        return id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
