package edu.sdle;

import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

public class HashRing {
    private static final XXHash64 HASHER = XXHashFactory.fastestInstance().hash64();
    private static final long SEED = 0L;

    private final NavigableMap<Long, UUID> ring;
    private final int vnodesPerNode;

    public HashRing(int vnodesPerNode) {
        ring = new TreeMap<>();
        this.vnodesPerNode = vnodesPerNode;
    }

    /**
     * Get the node associated with a key.
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
     * Add a node to the hash ring.
     * @param nodeId Unique ID of the node
     */
    public void addNode(UUID nodeId) {
        for (int i = 0; i < vnodesPerNode; i++) {
            long h = hashVNode(nodeId, i);
            ring.put(h, nodeId);
        }
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
        return new java.util.HashSet<>(ring.values()).size();
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
