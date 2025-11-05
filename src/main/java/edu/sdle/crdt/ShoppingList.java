package edu.sdle.crdt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sdle.Client;

import java.util.UUID;

/// A shopping list, implemented as a CRDT.
public class ShoppingList implements ICRDT<ShoppingList> {

    public final UUID id;
    public final LWWRegister<String> name;
    public final AWMap<String, PNCounter> items;

    // Clients who have viewed the shopping list
    public final GSet<Client> viewers;

    public ShoppingList(String name, Client creator) {
        this.id = UUID.randomUUID();
        this.name = new LWWRegister<>(name, creator.id().toString());
        this.items = new AWMap<>();
        this.viewers = new GSet<>();
        viewers.add(creator);
    }

    @JsonCreator
    public ShoppingList(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") LWWRegister<String> name,
            @JsonProperty("items") AWMap<String, PNCounter> items,
            @JsonProperty("viewers") GSet<Client> viewers
    ) {
        this.id = id;
        this.name = name;
        this.viewers = viewers;
        this.items = items;
    }

    // Merge a shopping list into this one
    @Override
    public void merge(ShoppingList other) {
        name.merge(other.name);
        viewers.merge(other.viewers);
        items.merge(other.items);
    }
}
