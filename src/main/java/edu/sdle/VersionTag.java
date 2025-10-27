package edu.sdle;

public class VersionTag {

    private final String nodeId;
    private final int count;

    public VersionTag(String nodeId, int count) {
        this.nodeId = nodeId;
        this.count = count;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        VersionTag other = (VersionTag) obj;
        return count == other.count && nodeId.equals(other.nodeId);
    }

    @Override
    public String toString() {
        return "(" + nodeId + ", " + count + ")";
    }
}
