package edu.sdle.crdt.counters;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.sdle.crdt.ICRDT;

import java.util.UUID;

/// A positive-negative counter
public class PNCounter implements ICRDT<PNCounter> {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    private final GCounter inc = new GCounter();

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    private final GCounter dec = new GCounter();

    // Get the value of the counter
    public Integer value() {
        return inc.value() - dec.value();
    }

    // Increment the counter for a node
    public void inc(UUID nodeId) {
        inc.inc(nodeId);
    }

    // Decrement the counter for a node
    public void dec(UUID nodeId) {
        dec.inc(nodeId);
    }

    // Merge a PNCounter into this one
    @Override
    public void merge(PNCounter other) {
        inc.merge(other.inc);
        dec.merge(other.dec);
    }

    @Override
    public String toString() {
        return "PNCounter(" + value() + ")";
    }
}
