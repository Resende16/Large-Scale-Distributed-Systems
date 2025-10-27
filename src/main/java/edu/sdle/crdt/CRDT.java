package edu.sdle.crdt;

// A conflict-free replicated data type with a value of type T.
public interface CRDT<T> {
    T value();
}
