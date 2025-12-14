package edu.sdle;

import edu.sdle.hashring.DistributedHashRing;
import edu.sdle.storage.Storage;

public record NodeState(DistributedHashRing ring, Storage storage, HintStorage hintStorage, Node node) {
}
