package edu.sdle.hashring;

import edu.sdle.crdt.ICRDT;
import edu.sdle.crdt.collections.AWSet;

import java.util.List;
import java.util.UUID;

/// A CRDT implementation of a hash ring
public class DistributedHashRing implements ICRDT<DistributedHashRing> {
    private final HashRing ring;
    private final AWSet<UUID> members;

    public DistributedHashRing(int vnodesPerNode) {
        ring = new HashRing(vnodesPerNode);
        members = new AWSet<>();
    }

    /**
     * Get the node associated with a key.
     * @param key Key to hash
     * @return ID of a node, or null if the ring is empty
     */
    public UUID getNode(UUID key) {
        return ring.getNode(key);
    }

    /**
     * Get the first N distinct physical nodes responsible for a key.
     * @param key Key to hash
     * @param N Number of nodes to return
     * @return An ArrayList of node IDs
     */
    public List<UUID> getNNodes(UUID key, int N) {
        return ring.getNNodes(key, N);
    }

    /**
     * Add a node to the hash ring.
     * @param nodeToAdd ID of the node to be added
     * @param issuerNode ID of the node who issued the operation
     * @return True if the node was not already in the ring
     */
    public boolean addNode(UUID nodeToAdd, UUID issuerNode) {
        if (members.contains(nodeToAdd))
            return false;

        members.add(nodeToAdd, issuerNode);
        return ring.addNode(nodeToAdd);
    }

    /**
     * Remove a node from the hash ring.
     * @param nodeToRemove Unique ID of the node
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

    @Override
    public void merge(DistributedHashRing other) {
        // Merge membership set
        var before = new java.util.HashSet<>(members.value());
        members.merge(other.members);

        // Merge actual ring

        for (var e : before) {
            if (!members.contains(e)) // removed
                ring.removeNode(e);
        }
        for (var e : members.value()) {
            if (!before.contains(e)) // added
                ring.addNode(e);
        }
    }
}
