package edu.sdle.crdt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sdle.User;
import edu.sdle.crdt.collections.AWMap;
import edu.sdle.crdt.collections.GSet;
import edu.sdle.crdt.lww.LWWRegister;

import java.util.UUID;

/// A shopping list, implemented as a CRDT.
public class ShoppingList implements ICRDT<ShoppingList> {
    @JsonProperty private final UUID id;
    @JsonProperty private final LWWRegister<String> name;
    @JsonProperty private final AWMap<String, ItemQuantity> items;

    // Clients who have viewed the shopping list
    @JsonProperty private final GSet<User> viewers;

    public ShoppingList(String name, User creator) {
        this.id = UUID.randomUUID();
        this.name = new LWWRegister<>(name, creator.id());
        this.items = new AWMap<>();
        this.viewers = new GSet<>();
        viewers.add(creator);
    }

    @JsonCreator
    private ShoppingList(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") LWWRegister<String> name,
            @JsonProperty("items") AWMap<String, ItemQuantity> items,
            @JsonProperty("viewers") GSet<User> viewers) {
        this.id = id;
        this.name = name;
        this.viewers = viewers;
        this.items = items;
    }

    @JsonIgnore
    public UUID getId() {
        return id;
    }

    @JsonIgnore
    public String getName() {
        return name.value();
    }

    @JsonIgnore
    public GSet<User> getViewers() {
        return viewers;
    }

    @JsonIgnore
    public AWMap<String, ItemQuantity> getItems() {
        return items;
    }

    public void setName(String name, UUID node) {
        this.name.assign(name, node);
    }

    // Merge a shopping list into this one
    @Override
    public void merge(ShoppingList other) {
        name.merge(other.name);
        viewers.merge(other.viewers);
        items.merge(other.items);
    }
}
