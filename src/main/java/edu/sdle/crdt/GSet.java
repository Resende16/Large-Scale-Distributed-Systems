package edu.sdle.crdt;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;

/// A grow-only set
public class GSet<T> implements ICRDT<GSet<T>> {

    @JsonProperty
    private final Set<T> set = new HashSet<>();

    public Set<T> value() {
        return Set.copyOf(set);
    }

    public boolean add(T element) {
        return set.add(element);
    }

    public boolean contains(T element) {
        return set.contains(element);
    }

    @Override
    public void merge(GSet<T> other) {
        set.addAll(other.set);
    }

    @Override
    public String toString() {
        return "GSet(" + set + ")";
    }
}
