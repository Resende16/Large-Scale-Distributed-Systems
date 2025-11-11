package edu.sdle.hashring;

import edu.sdle.Node;
import edu.sdle.crdt.ICRDT;
import edu.sdle.crdt.collections.AWMap;
import edu.sdle.crdt.lww.LWWRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/// A CRDT implementation of a hash ring
public class DistributedHashRing implements ICRDT<DistributedHashRing> {
    private final HashRing ring;
    private final AWMap<UUID, LWWRegister<String>> members; // ID -> ip:port

    public DistributedHashRing(int vnodesPerNode) {
        ring = new HashRing(vnodesPerNode);
        members = new AWMap<>();
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

        var netAddress = members.get(nodeId);
        return new Node(nodeId, netAddress.value());
    }

    /**
     * Get the first N distinct physical nodes responsible for a key.
     * @param key Key to hash
     * @param N Number of nodes to return
     * @return An ArrayList of Node objects
     */
    public List<Node> getNNodes(UUID key, int N) {
        var nodeIds = ring.getNNodes(key, N);
        List<Node> nodes = new ArrayList<>(nodeIds.size());

        for (var id : nodeIds)
            nodes.add(new Node(id, members.get(id).value()));
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

        if (_node != null)
            // Simply update the network address
            _node.assign(node.getNetAddress(), issuerNode);
        else {
            members.put(node.getId(),
                    new LWWRegister<>(node.getNetAddress(), issuerNode),
                    issuerNode
            );
            ring.addNode(node.getId());
        }

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
