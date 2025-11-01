package edu.sdle.crdt;

/// An Add-Wins CRDT Hashmap; value type must be a CRDT
public class AWMap<K,V extends CRDT<V>> implements CRDT<AWMap<K, V>> {

    private final AWSet<AWMapEntry<K, V>> awSet = new AWSet<>();

    // Get the value associated with a key
    public V get(K key) {
        for (var e : awSet.value()) {
            if (e.key().equals(key)) {
                return e.value();
            }
        }
        return null;
    }

    // Map a key to a value in the map
    public void put(K key, V value, String nodeId) {
        var entry = new AWMapEntry<>(key, value);
        awSet.add(entry, nodeId);
    }

    // Check if the map contains a key
    public boolean contains(K key) {
        return awSet.contains(new AWMapEntry<>(key));
    }

    // Remove the mapping for a key from the map
    public boolean remove(K key) {
        return awSet.remove(new AWMapEntry<>(key));
    }

    // Merge a AWMap with this one
    @Override
    public void merge(AWMap<K, V> other) {
        // Merge key set first
        awSet.merge(other.awSet);

        // Merge values
        for (var otherEntry : other.awSet.value()) {
            var entry = this.get(otherEntry.key());
            if (entry != null)
                entry.merge(otherEntry.value());
        }
    }

    @Override
    public String toString() {
        if (awSet.value().isEmpty())
            return "AWMap(map={})";

        StringBuilder sb = new StringBuilder("AWMap(map={\n");
        for (var e : awSet.value()) {
            sb.append("  ")
                    .append(e.key())
                    .append("=").append(e.value())
                    .append("\n");
        }
        sb.append("})");
        return sb.toString();
    }
}
