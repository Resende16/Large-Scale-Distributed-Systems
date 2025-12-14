package edu.sdle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sdle.hashring.DistributedHashRing;

/// Represents the complete and replicated state of a proxy node
public record ProxyState(@JsonProperty DistributedHashRing ring, int timeout, int N) {
    @JsonCreator
    public ProxyState(@JsonProperty("ring") DistributedHashRing ring,
                      @JsonProperty("timeout") int timeout,
                      @JsonProperty("n") int N) {
        this.ring = ring;
        this.timeout = timeout;
        this.N = N;
    }
}
