package edu.sdle.crdt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class Timestamp {

    @JsonProperty private String node; // tiebreaker
    @JsonProperty private int count;

    public Timestamp(String node) {
        this.node = node;
        this.count = 0;
    }

    public void inc(String node) {
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

    //@JsonValue // serializer
    public String toString() {
        return node + ":" + count;
    }

    //@JsonCreator // deserializer
    private static Timestamp fromJSON(String str) {
        String[] k = str.split(":");
        if (k.length != 2)
            throw new IllegalArgumentException("Invalid timestamp");

        Timestamp t = new Timestamp(k[0]);
        t.count = Integer.parseInt(k[1]);
        return t;
    }
}
