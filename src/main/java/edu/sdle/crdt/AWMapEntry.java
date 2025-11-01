package edu.sdle.crdt;

import java.util.Objects;

public class AWMapEntry<K,V extends CRDT<V>> {

    private final K key;
    private final V value;

    // Useful for == comparisons, where the value is redundant
    public AWMapEntry(K key) {
        this.key = key;
        this.value = null;
    }

    public AWMapEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K key() {
        return key;
    }

    public V value() {
        return value;
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
