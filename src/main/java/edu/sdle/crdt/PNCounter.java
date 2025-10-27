package edu.sdle.crdt;

import java.util.HashMap;
import java.util.Map;

public class PNCounter implements CRDT<Integer> {

    private final HashMap<String, Integer> inc;
    private final HashMap<String, Integer> dec;

    public PNCounter() {
        this.inc = new HashMap<>();
        this.dec = new HashMap<>();
    }

    // Get the value of the counter
    @Override
    public Integer value() {
        int inc = 0, dec = 0;

        for (int count : this.inc.values()) {
            inc += count;
        }
        for (int count : this.dec.values()) {
            dec += count;
        }

        return inc - dec;
    }

    // Increment the counter for a node
    public void inc(String nodeId) {
        inc.put(nodeId, inc.getOrDefault(nodeId, 0) + 1);
    }

    // Decrement the counter for a node
    public void dec(String nodeId) {
        dec.put(nodeId, dec.getOrDefault(nodeId, 0) + 1);
    }

    // Merge a PNCounter into this one
    public void merge(PNCounter other) {
        for (Map.Entry<String, Integer> entry : other.inc.entrySet()) {
            inc.put(entry.getKey(), Math.max(inc.getOrDefault(entry.getKey(), 0), entry.getValue()));
        }
        for (Map.Entry<String, Integer> entry : other.dec.entrySet()) {
            dec.put(entry.getKey(), Math.max(dec.getOrDefault(entry.getKey(), 0), entry.getValue()));
        }
    }
}
