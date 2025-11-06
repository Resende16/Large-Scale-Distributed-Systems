package edu.sdle.crdt.collections;

import edu.sdle.crdt.ICRDT;

import java.util.Objects;

public record AWMapEntry<K, V extends ICRDT<V>>(K key, V value) {

    // Useful for == comparisons, where the value is redundant
    public AWMapEntry(K key) {
        this(key, null);
    }

    // The equals method compares only the keys, allowing us to
    // store both key and value as a single entry of an AWSet, i.e.
    // no need for an additional data structure to map keys to values
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AWMapEntry<?, ?> other = (AWMapEntry<?, ?>) o;
        return Objects.equals(key, other.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
