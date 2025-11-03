package edu.sdle.crdt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sdle.Client;

import java.util.UUID;

/// A shopping list, implemented as a CRDT.
public class ShoppingList implements ICRDT<ShoppingList> {

    public final UUID id;
    public final String name;

    // Clients who have accessed the shopping list
    public final AWSet<Client> accessors;

    public final AWMap<String, PNCounter> items;

    public ShoppingList(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.items = new AWMap<>();
        this.accessors = new AWSet<>();
    }

    @JsonCreator
    public ShoppingList(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") String name,
            @JsonProperty("accessors") AWSet<Client> accessors,
            @JsonProperty("items") AWMap<String, PNCounter> items
    ) {
        this.id = id;
        this.name = name;
        this.accessors = accessors;
        this.items = items;
    }

    // Merge a shopping list into this one
    @Override
    public void merge(ShoppingList other) {
        accessors.merge(other.accessors);
        items.merge(other.items);
    }
}
