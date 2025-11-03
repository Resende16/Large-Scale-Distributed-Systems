package edu.sdle.crdt;

/// A Last-writer-wins register
public class LWWRegister<T> implements ICRDT<LWWRegister<T>> {

    private T value;
    private Timestamp clock;

    public LWWRegister(T value) {
        this.value = value;
    }

    public T value() {
        return value;
    }

    public void assign(T value, String nodeId) {
        clock.inc(nodeId);
        this.value = value;
    }

    @Override
    public void merge(LWWRegister<T> other) {
        if (clock.happenedBefore(other.clock)) {
            value = other.value;
            clock = other.clock;
        }
    }
}
