package edu.sdle.hashring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sdle.Node;
import edu.sdle.crdt.ICRDT;
import edu.sdle.crdt.collections.AWMap;
import edu.sdle.crdt.lww.LWWRegister;
import edu.sdle.utils.JsonUtils;

import java.util.*;

/// A CRDT implementation of a hash ring
public class DistributedHashRing implements ICRDT<DistributedHashRing> {
    @JsonIgnore private final HashRing ring;
    @JsonIgnore private final int R; // Replication factor
    @JsonProperty public final AWMap<UUID, RingNode> members;

    public DistributedHashRing(int vnodesPerNode, int R) {
        ring = new HashRing(vnodesPerNode);
        members = new AWMap<>();
        this.R = R;
    }

    // For deserialization
    @JsonCreator
    private DistributedHashRing(
            @JsonProperty("members") AWMap<UUID, RingNode> members,
            @JsonProperty("vnodes") int vnodes,
            @JsonProperty("R") int R
    ) {
        this.R = R;
        this.members = members;
        this.ring = new HashRing(vnodes);

        for (var e : members.entrySet()) {
            ring.addNode(e.key());
        }
    }

    /**
     * Get the node associated with a key.
     * @param key Key to hash
     * @return A Node object, or null if the ring is empty
     */
    public Node getNode(UUID key) {
        var nodeId = ring.getNode(key);
        if (nodeId == null)
            return null;

        var node = members.get(nodeId);
        return new Node(nodeId, node.getZmqAddress());
    }

    // Shortcut, since distributed version contains R
    public List<Node> getNNodes(UUID key) {
        return getNNodes(key, R);
    }

    /**
     * Get the first N distinct physical nodes responsible for a key.
     * @param key Key to hash
     * @param N Number of nodes to return
     * @return A Set of Node objects
     */
    public List<Node> getNNodes(UUID key, int N) {
        var nodeIds = ring.getNNodes(key, N);
        List<Node> nodes = new ArrayList<>(nodeIds.size());

        for (var id : nodeIds)
            nodes.add(new Node(id, members.get(id).getZmqAddress()));
        return nodes;
    }

    /**
     * Add a node to the hash ring. If a node with the same id is
     * already in the ring, its network address will be updated.
     * @param node Node to be added
     * @param issuerNode ID of the node who issued the operation
     */
    public void addNode(Node node, UUID issuerNode) {
        var _node = members.get(node.getId());

        //mostly assuming node id is the same as the issuer
        //since nodes can only add themselves

        if (_node != null)
            // Simply update the network address
            _node.updateAddress(node.getZMQAddress(), issuerNode);
        else {
            members.put(node.getId(),
                    new RingNode(node.getId(), node.getZMQAddress()),
                    issuerNode
            );
            ring.addNode(node.getId());
        }

    }

    /**
     * Remove a node from the hash ring.
     * @param nodeToRemove Unique ID of the node
     * @return True if the ring contained the element
     */
    public boolean removeNode(UUID nodeToRemove) {
        if (!members.remove(nodeToRemove))
            return false;

        ring.removeNode(nodeToRemove);
        return true;
    }

    /**
     * Check if the hash ring contains a node.
     * @param nodeId Unique ID of the node
     * @return True if the ring contains the node
     */
    public boolean containsNode(UUID nodeId) {
        return members.contains(nodeId);
    }

    /**
     * Get the number of virtual nodes in the ring.
     * @return Size of the hash ring
     */
    public int vnodeCount() {
        return ring.vnodeCount();
    }

    /**
     * Get the number of physical nodes in the ring.
     * @return Number of nodes in the ring
     */
    public int nodeCount() {
        return ring.nodeCount();
    }

    /**
     * Get the number of virtual nodes per physical node.
     * @return Number of vnodes per physical node
     */
    @JsonProperty("vnodes")
    public int getVnodesPerNode() {
        return ring.getVnodesPerNode();
    }

    /**
     * Get the replication factor of the distributed ring.
     * @return Parameter R of the distributed hash ring
     */
    @JsonProperty("R")
    public int getR() {
        return R;
    }

    // could be a Set, but a List of more useful to the caller
    @JsonIgnore
    public List<Node> getNodes() {
        List<Node> nodes = new ArrayList<>(members.size());

        for (var entry : members.entrySet()) {
            var nodeId = entry.key();
            var node = entry.value();

            nodes.add(new Node(nodeId, node.getZmqAddress()));
        }

        return nodes;
    }

    public RingNode getRingNode(UUID key) {
        return members.get(key);
    }

    // Clever way to easily copy a distributed ring
    //todo: Implement without relying on Json serialization
    public DistributedHashRing copy() {
        var jsonString = JsonUtils.toJson(this);
        return JsonUtils.fromJson(jsonString, DistributedHashRing.class);
    }

    @Override
    public void merge(DistributedHashRing other) {
        // Merge membership set
        var before = members.entrySet();
        members.merge(other.members);
        var after = members.entrySet();

        // Merge actual ring

        for (var e : before) {
            if (!after.contains(e)) // removed
                ring.removeNode(e.key());
        }
        for (var e : after) {
            if (!before.contains(e)) // added
                ring.addNode(e.key());
        }
    }
}
