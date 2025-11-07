package edu.sdle.crdt.lww;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.UUID;

public class Timestamp {
    private UUID node; // tiebreaker
    private int count;

    public Timestamp(UUID node) {
        this.node = node;
        this.count = 0;
    }

    // For deserialization; uid is node:count
    @JsonCreator
    private Timestamp(String uid) {
        String[] k = uid.split(":");
        if (k.length != 2)
            throw new IllegalArgumentException("Invalid uid");

        node = UUID.fromString(k[0]);
        count = Integer.parseInt(k[1]);
    }

    public void inc(UUID node) {
        count++;
        this.node = node;
    }

    public int getCount() {
        return count;
    }

    public boolean happenedBefore(Timestamp other) {
        return count < other.count ||
                (count == other.count && node.compareTo(other.node) < 0);
    }

    @JsonValue
    public String toString() {
        return node + ":" + count;
    }
}
