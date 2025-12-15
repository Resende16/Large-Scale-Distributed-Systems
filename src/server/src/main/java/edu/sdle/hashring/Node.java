package edu.sdle.hashring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sdle.crdt.ICRDT;
import edu.sdle.crdt.lww.LWWRegister;
import edu.sdle.crdt.lww.Timestamp;

import java.sql.Time;
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
    private final BiasedLWWRegister<NodeStatus> status;

    @JsonProperty
    private final LWWRegister<Integer> consecutiveFailures;

    @JsonProperty
    private final LWWRegister<Long> lastSeen; // in ms

    public Node(UUID nodeId, String nodeAddr) {
        id = nodeId;
        this.address = new LWWRegister<>(nodeAddr, nodeId);
        this.status = new BiasedLWWRegister<>(UP, nodeId, nodeId);
        this.lastSeen = new LWWRegister<>(System.currentTimeMillis(), nodeId);
        this.consecutiveFailures = new LWWRegister<>(0, nodeId);
    }

    // For deserialization
    @JsonCreator
    private Node(
            @JsonProperty("address") LWWRegister<String> address,
            @JsonProperty("status") BiasedLWWRegister<NodeStatus> status,
            @JsonProperty("lastSeen") LWWRegister<Long> lastSeen,
            @JsonProperty("consecutiveFailures") LWWRegister<Integer> consecutiveFailures
    ) {
        this.address = address;
        this.status = status;
        this.lastSeen = lastSeen;
        this.consecutiveFailures = consecutiveFailures;
    }

    @JsonIgnore
    public UUID getId() {
        return id;
    }

    @JsonIgnore
    public Timestamp getTS(){
        return status.timestamp();
    }

    @JsonIgnore
    public String getZmqAddress() {
        return address.value();
    }

    @JsonIgnore
    public NodeStatus getStatus() {
        return status.value();
    }

    @JsonIgnore
    public long getLastSeen() {
        return lastSeen.value();
    }

    @JsonIgnore
    public int getConsecutiveFailures() {
        return consecutiveFailures.value();
    }

    public void updateAddress(String newAddr, UUID issuerNode) {
        address.assign(newAddr, issuerNode);
    }

    // same as marking UP
    public void keepAlive(UUID issuerNode) {
        status.assign(UP, issuerNode);
        lastSeen.assign(System.currentTimeMillis(), issuerNode);
        consecutiveFailures.assign(0, issuerNode);
    }

    public void markSuspected(UUID issuerNode) {
        if (status.value() == UP)
            status.assign(SUSPECTED, issuerNode);

        consecutiveFailures.assign(consecutiveFailures.value() +1, issuerNode);
    }

    public void markDown(UUID issuerNode) {
        status.assign(DOWN, issuerNode);
        consecutiveFailures.assign(consecutiveFailures.value() +1, issuerNode);
    }

    @Override
    public void merge(Node other) {
        address.merge(other.address);
        status.merge(other.status);
        lastSeen.merge(other.lastSeen);
        consecutiveFailures.merge(other.consecutiveFailures);
    }

    @Override
    public String toString() {
        return "RingNode{" +
                "address=" + address.value() +
                ", status=" + status.value() +
                ", failures=" + consecutiveFailures.value() +
                ", lastSeen=" + (System.currentTimeMillis() - lastSeen.value()) + "ms ago" +
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
