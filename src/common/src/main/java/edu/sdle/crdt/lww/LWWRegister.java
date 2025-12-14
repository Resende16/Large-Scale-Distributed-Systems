package edu.sdle.crdt.lww;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sdle.crdt.ICRDT;

import java.util.UUID;

/// A Last-writer-wins register
public class LWWRegister<T> implements ICRDT<LWWRegister<T>> {

    @JsonProperty protected T value;
    @JsonProperty protected Timestamp timestamp;

    public LWWRegister(T value, UUID node) {
        this.value = value;
        this.timestamp = new Timestamp(node);
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

    public Timestamp timestamp() {
        return timestamp;
    }

    public void assign(T value, UUID node) {
        timestamp.inc(node);
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
