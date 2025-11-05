package edu.sdle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record Client(UUID id, String name) {

    public Client(final String name) {
        this(UUID.randomUUID(), name);
    }

    @JsonCreator
    public Client(@JsonProperty("id") final UUID id,
                  @JsonProperty("name") final String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return id + ":" + name;
    }
}