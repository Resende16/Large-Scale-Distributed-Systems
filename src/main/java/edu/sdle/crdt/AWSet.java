package edu.sdle.crdt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/// An Add-Wins CRDT set, also known as an OR set
public class AWSet<T> implements CRDT<AWSet<T>> {

    private final Map<T, Map<String, Integer>> items = new HashMap<>();
    private final Map<String, Integer> versionDots =  new HashMap<>();

    public Set<T> value() {
        return items.keySet();
    }

    /**
     * Add an item to the set.
     * @param item Item to be added
     * @param nodeId ID of the node responsible for the add
     */
    public void add(T item, String nodeId) {
        // Increment node dot
        int counter = versionDots.getOrDefault(nodeId, 0);
        versionDots.put(nodeId, ++counter);

        // Add item to the set if absent + update dot
        items.computeIfAbsent(item, _ -> new HashMap<>())
                .put(nodeId, counter);
    }

    /**
     * Remove an item from the set.
     * @param item Item to be removed
     * @return True if the set contained the item
     */
    public boolean remove(T item) {
        return items.remove(item) != null;
    }

    /**
     * Check if the set contains an item.
     * @param item Item to check for
     * @return True if the set contains the item
     */
    public boolean contains(T item) {
        return items.containsKey(item);
    }

    /**
     * Merge another AWSet into this one.
     * @param other AWSet to merge with
     */
    @Override
    public void merge(AWSet<T> other) {
        mergeRemovedDots(other);
        mergeNewDots(other);

        // Merge dot map
        for (var e : other.versionDots.entrySet()) {
            versionDots.put(e.getKey(), Math.max(versionDots.getOrDefault(e.getKey(), 0), e.getValue()));
        }
    }

    /**
     * Merge new dots (and items) not yet observed locally.
     * @param other AWSet to merge with
     */
    private void mergeNewDots(AWSet<T> other) {
        for (var e : other.items.entrySet()) {
            var otherItem = e.getKey();
            var otherDots = e.getValue();

            Map<String, Integer> newDots = new HashMap<>();

            // Track dots we haven't seen
            for (var dot : otherDots.entrySet()) {
                var otherNode = dot.getKey();
                var otherCount = dot.getValue();
                var localCount = versionDots.getOrDefault(otherNode, 0);

                if (otherCount > localCount)
                    newDots.put(dot.getKey(), dot.getValue());
            }

            if (newDots.isEmpty())
                continue;

            // Create item if missing and update its dots
            items.computeIfAbsent(otherItem, _ -> new HashMap<>())
                    .putAll(newDots);
        }
    }

    /**
     * Merge dots that exist locally but were removed by other nodes.
     * @param other AWSet to merge with
     */
    private void mergeRemovedDots(AWSet<T> other) {
        var it = items.entrySet().iterator();

        while (it.hasNext()) { // For each item
            var e = it.next();
            var item = e.getKey();
            var dots = e.getValue();

            var it2 = dots.entrySet().iterator();

            while (it2.hasNext()) { // For each of its dots
                var dot = it2.next();
                var node = dot.getKey();
                var count = dot.getValue();

                var otherDots = other.items.get(item); // null if item was removed
                var otherCount = other.versionDots.getOrDefault(node, 0);

                // Item dot was removed by another node
                if (count <= otherCount && (otherDots == null || !otherDots.containsKey(node)))
                    it2.remove();
            }

            // No dots, thus the item is removed
            if (dots.isEmpty())
                it.remove();
        }
    }

    @Override
    public String toString() {
        return "AWSet(value=" + value() + ")";
    }
}
