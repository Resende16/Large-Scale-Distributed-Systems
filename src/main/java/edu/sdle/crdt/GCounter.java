package edu.sdle.crdt;

import java.util.HashMap;
import java.util.Map;

/// A grow-only CRDT counter
public class GCounter implements CRDT<GCounter> {

    private final Map<String, Integer> count =  new HashMap<>();

    // Get the current value of the counter
    public Integer value() {
        int sum = 0;
        for (Integer v : count.values()) {
            sum += v;
        }
        return sum;
    }

    // Increment the counter for a node
    public void inc(String nodeId) {
        count.put(nodeId, count.getOrDefault(nodeId, 0) + 1);
    }

    // Merge another GCounter into this one
    @Override
    public void merge(GCounter other) {
        for (var e : other.count.entrySet()) {
            count.put(e.getKey(), Math.max(count.getOrDefault(e.getKey(), 0), e.getValue()));
        }
    }

    @Override
    public String toString() {
        return "GCounter(value=" + value() + ")";
    }
}
