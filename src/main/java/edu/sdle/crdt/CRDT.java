package edu.sdle.crdt;

// A conflict-free replicated data type
public interface CRDT<T extends CRDT<T>> {
    /**
     * Merge another CRDT into this one; types must match
     * @param other CRDT to merge with
     */
    void merge(T other);
}
