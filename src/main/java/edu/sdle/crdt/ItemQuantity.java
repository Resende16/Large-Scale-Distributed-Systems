package edu.sdle.crdt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sdle.crdt.counters.PNCounter;

import java.util.UUID;

/// ItemQuantity holds the current quantity + the target, i.e.
/// max quantity; Bound constraints only hold locally
public class ItemQuantity implements ICRDT<ItemQuantity> {
    @JsonProperty private final PNCounter current;
    @JsonProperty private final int max;
    // min=0

    public ItemQuantity(int max) {
        this.current = new PNCounter();
        this.max = max;
    }

    @JsonCreator
    public ItemQuantity(
            @JsonProperty("current") PNCounter current,
            @JsonProperty("max") int max
    ) {
        this.current = current;
        this.max = max;
    }

    @JsonIgnore
    public int getCurrent() {
        return current.value();
    }

    @JsonIgnore
    public int getMax() {
        return max;
    }

    public boolean inc(UUID nodeId) {
        if (current.value() >= max)
            return false;

        current.inc(nodeId);
        return true;
    }

    public boolean dec(UUID nodeId) {
        if (current.value() <= 0)
            return false;

        current.dec(nodeId);
        return true;
    }

    @Override
    public void merge(ItemQuantity other) {
        current.merge(other.current);
    }

    @Override
    public String toString() {
        return "ItemQuantity(" + current.value() + "/" + max + ")";
    }
}
