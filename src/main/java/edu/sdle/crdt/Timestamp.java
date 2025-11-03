package edu.sdle.crdt;

public class Timestamp {

    private String nodeId; // tiebreaker
    private int count;

    public Timestamp(String nodeId) {
        this.nodeId = nodeId;
        this.count = 0;
    }

    public void inc(String nodeId) {
        count++;
        this.nodeId = nodeId;
    }

    public int getCount() {
        return count;
    }

    public boolean happenedBefore(Timestamp other) {
        return count < other.count ||
                (count == other.count && nodeId.compareTo(other.nodeId) < 0);
    }
}
