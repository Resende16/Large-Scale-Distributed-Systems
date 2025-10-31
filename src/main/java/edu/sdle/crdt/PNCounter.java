package edu.sdle.crdt;

/// A positive-negative CRDT counter
public class PNCounter implements CRDT<PNCounter> {

    private final GCounter inc = new GCounter();
    private final GCounter dec = new GCounter();

    // Get the value of the counter
    public Integer value() {
        return inc.value() - dec.value();
    }

    // Increment the counter for a node
    public void inc(String nodeId) {
        inc.inc(nodeId);
    }

    // Decrement the counter for a node
    public void dec(String nodeId) {
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
        return "PNCounter(value=" + value() + ")";
    }
}
