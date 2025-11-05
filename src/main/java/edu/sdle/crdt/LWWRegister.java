package edu.sdle.crdt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/// A Last-writer-wins register
public class LWWRegister<T> implements ICRDT<LWWRegister<T>> {

    @JsonProperty private T value;
    @JsonProperty private Timestamp timestamp;

    public LWWRegister(T value, String nodeId) {
        this.value = value;
        this.timestamp = new Timestamp(nodeId);
    }

    @JsonCreator
    public LWWRegister(@JsonProperty("value") T value,
                       @JsonProperty("timestamp") Timestamp timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public T value() {
        return value;
    }

    public void assign(T value, String nodeId) {
        timestamp.inc(nodeId);
        this.value = value;
    }

    @Override
    public void merge(LWWRegister<T> other) {
        if (timestamp.happenedBefore(other.timestamp)) {
            value = other.value;
            timestamp = other.timestamp;
        }
    }

    @Override
    public String toString() {
        return "LWWRegister(" + value + ")";
    }
}
