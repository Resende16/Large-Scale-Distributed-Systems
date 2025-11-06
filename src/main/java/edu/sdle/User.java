package edu.sdle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
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
        return "Client(" + id + ", " + name + ")";
    }

    // Ignore name when comparing
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        User other = (User) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}