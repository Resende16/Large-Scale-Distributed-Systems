package edu.sdle.hashring;

import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;

import java.util.*;

public class HashRing {
    private static final XXHash64 HASHER = XXHashFactory.fastestInstance().hash64();
    private static final long SEED = 0L;

    private final NavigableMap<Long, UUID> ring;
    private final int vnodesPerNode; // virtual nodes per physical node

    public HashRing(int vnodesPerNode) {
        ring = new TreeMap<>();
        this.vnodesPerNode = vnodesPerNode;
    }

    /**
     * Get the node responsible for a key.
     * @param key Key to hash
     * @return ID of a node, or null if the ring is empty
     */
    public UUID getNode(UUID key) {
        if (ring.isEmpty())
            return null;
        long h = hashUUID(key);
        var e = ring.ceilingEntry(h);
        if (e == null) // wrap-around
            e = ring.firstEntry();
        return e.getValue();
    }

    /**
     * Get the first N distinct physical nodes responsible for a key.
     * @param key Key to hash
     * @param N Number of nodes to return
     * @return An ArrayList of node IDs
     */
    public List<UUID> getNNodes(UUID key, int N) {
        if (ring.isEmpty())
            return List.of();

        long h = hashUUID(key);
        var result = new ArrayList<UUID>(N);
        var seen = new HashSet<UUID>();

        var iter = ring.tailMap(h, true).entrySet().iterator();
        var wrapped = false;

        // if there are <N physical nodes, we do a full cycle
        // from h to END, and then START to h
        while (result.size() < N && (!wrapped || iter.hasNext())) {
            if (!iter.hasNext()) {
                iter = ring.headMap(h, false).entrySet().iterator();
                wrapped = true;
                continue;
            }

            var node = iter.next().getValue();
            if (seen.add(node))
                result.add(node);
        }
        return result;
    }

    /**
     * Add a node to the hash ring.
     * @param nodeId Unique ID of the node
     * @return True if the node was not already in the ring
     */
    public boolean addNode(UUID nodeId) {
        if (ring.containsValue(nodeId))
            return false;

        for (int i = 0; i < vnodesPerNode; i++) {
            long h = hashVNode(nodeId, i);
            ring.put(h, nodeId);
        }
        return true;
    }

    /**
     * Remove a node from the hash ring.
     * @param nodeId Unique ID of the node
     */
    public void removeNode(UUID nodeId) {
        for (int i = 0; i < vnodesPerNode; i++) {
            long h = hashVNode(nodeId, i);
            ring.remove(h);
        }
    }

    /**
     * Check if the hash ring contains a node.
     * @param nodeId Unique ID of the node
     * @return True if the ring contains the node
     */
    public boolean containsNode(UUID nodeId) {
        return ring.containsValue(nodeId);
    }

    /**
     * Get the number of virtual nodes in the ring.
     * @return Size of the hash ring
     */
    public int vnodeCount() {
        return ring.size();
    }

    /**
     * Get the number of physical nodes in the ring.
     * @return Number of nodes in the ring
     */
    public int nodeCount() {
        return new HashSet<>(ring.values()).size();
    }

    @Override
    public String toString() {
        return "HashRing{nodes=" + nodeCount() + ", " +
                "vnodes=" + vnodeCount() + '}';
    }

    // Utility method to hash a UUID object
    private static long hashUUID(UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        // Little-endian, doesn't really matter
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (i * 8));
            bytes[8 + i] = (byte) (lsb >>> (i * 8));
        }
        return HASHER.hash(bytes, 0, 16, SEED);
    }

    // Utility method to hash a UUID along with an int
    private static long hashVNode(UUID nodeId, int vnodeIndex) {
        byte[] bytes = new byte[20];
        long msb = nodeId.getMostSignificantBits();
        long lsb = nodeId.getLeastSignificantBits();

        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (i * 8));
            bytes[8 + i] = (byte) (lsb >>> (i * 8));
        }
        // copy over vnode index
        bytes[16] = (byte) vnodeIndex;
        bytes[17] = (byte) (vnodeIndex >>> 8);
        bytes[18] = (byte) (vnodeIndex >>> 16);
        bytes[19] = (byte) (vnodeIndex >>> 24);
        return HASHER.hash(bytes, 0, 20, SEED);
    }
}
