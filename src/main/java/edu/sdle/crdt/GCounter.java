package edu.sdle.crdt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/// A grow-only counter
public class GCounter implements ICRDT<GCounter> {

    private final Map<UUID, Integer> count;

    public GCounter() {
        this.count = new HashMap<>();
    }

    @JsonCreator // for deserializing
    private GCounter(Map<UUID, Integer> count) {
        this.count = new HashMap<>(count);
    }

    @JsonValue // for serializing
    private Map<UUID, Integer> asMap() {
        return count;
    }

    // Get the current value of the counter
    public Integer value() {
        int sum = 0;
        for (Integer v : count.values()) {
            sum += v;
        }
        return sum;
    }

    // Increment the counter for a node
    public void inc(UUID nodeId) {
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
        return "GCounter(" + value() + ")";
    }
}
