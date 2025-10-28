package edu.sdle.crdt;

// A conflict-free replicated data type which yields a value of type T.
public interface CRDT<T> {
    T value();
}
