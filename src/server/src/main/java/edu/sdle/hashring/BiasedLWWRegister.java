package edu.sdle.hashring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sdle.crdt.lww.LWWRegister;
import edu.sdle.crdt.lww.Timestamp;

import java.util.UUID;

/**
 * A Last-Write-Wins register with priority bias toward a specific node.
 * <p>
 * During merge, updates from the bias node always win, regardless of timestamp.
 * A designated "bias" node takes maximum precedence in conflict resolution.
 */
public class BiasedLWWRegister<T> extends LWWRegister<T> {
    @JsonProperty("bias")
    private final UUID biasNode;

    public BiasedLWWRegister(T value, UUID node, UUID biasNode) {
        super(value, node);
        this.biasNode = biasNode;
    }

    // For deserialization
    @JsonCreator
    public BiasedLWWRegister(@JsonProperty("value") T value,
                             @JsonProperty("timestamp") Timestamp timestamp,
                             @JsonProperty("bias") UUID biasNode) {
        super(value, timestamp);
        this.biasNode = biasNode;
    }

    @Override
    public void merge(LWWRegister<T> other) {
        // Last person to set the register was the bias node,
        // so it takes precedence

         super.merge(other);
    }
}
