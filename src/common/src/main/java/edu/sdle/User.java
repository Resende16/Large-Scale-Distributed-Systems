package edu.sdle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record User(UUID id, String name) {

    public User(final String name) {
        this(UUID.randomUUID(), name);
    }

    @JsonCreator
    public User(@JsonProperty("id") final UUID id,
                @JsonProperty("name") final String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "User(" + id + ", " + name + ")";
    }
}