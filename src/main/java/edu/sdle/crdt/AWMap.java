package edu.sdle.crdt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/// An Add-Wins CRDT Hashmap; value type must be a CRDT
public class AWMap<K,V extends CRDT<V>> implements CRDT<AWMap<K, V>> {

    private final AWSet<K> keySet = new AWSet<>();
    public final Map<K,V> map = new HashMap<>();

    // Get the value associated with a key
    public V get(K key) {
        return map.get(key);
    }

    // Check if the map contains a key
    public boolean containsKey(K key) {
        return keySet.contains(key);
    }

    // Map a key to a value in the map
    public void put(K key, V value, String nodeId) {
        keySet.add(key, nodeId);
        map.put(key, value);
    }

    // Remove the mapping for a key from the map
    public V remove(K key) {
        if (!keySet.remove(key))
            return null;
        return map.remove(key);
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    // Merge a AWMap with this one
    @Override
    public void merge(AWMap<K, V> other) {
        // Merge key set first
        keySet.merge(other.keySet);

        // Add new and merge existing values
        for (var key : keySet.value()) {
            V value = map.get(key);

            if (value == null) {
                // Map doesn't contain key -> it's a new entry
                if (!other.containsKey(key))
                    throw new IllegalStateException("Unmergeable state, AWMap is corrupted?");
                map.put(key, other.map.get(key));
            }
            else {
                V otherValue = other.map.get(key);
                if (otherValue != null)
                    // Merge with other
                    value.merge(otherValue);
            }
        }

        // Remove values with keys no longer present
        map.keySet().removeIf(key -> !keySet.contains(key));
    }

    @Override
    public String toString() {
        if (map.isEmpty())
            return "AWMap(map={})";

        StringBuilder sb = new StringBuilder("AWMap(map={\n");
        for (var e : map.entrySet()) {
            sb.append("  ")
                    .append(e.getKey())
                    .append("=").append(e.getValue())
                    .append("\n");
        }
        sb.append("})");
        return sb.toString();
    }
}
