package edu.sdle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sdle.hashring.DistributedHashRing;

/// Represents the complete and replicated state of a proxy node
public class ProxyState {
    @JsonProperty private final DistributedHashRing ring;

    // Timeout for client requests
    private final int timeout;

    // Replication factor
    private final int N;

    @JsonCreator
    public ProxyState(@JsonProperty("ring") DistributedHashRing ring,
                      @JsonProperty("timeout") int timeout,
                      @JsonProperty("n") int N) {
        this.ring = ring;
        this.timeout = timeout;
        this.N = N;
    }

    @JsonIgnore
    public DistributedHashRing getHashRing() {
        return ring;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getN() {
        return N;
    }
}