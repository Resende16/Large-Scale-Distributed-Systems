package edu.sdle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class Client {

    @JsonProperty
    private final UUID id;

    @JsonProperty
    private String name;

    public Client(final String name) {
        this(UUID.randomUUID(), name);
    }

    @JsonCreator
    public Client(final UUID id, final String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return id + ":" + name;
    }
}
