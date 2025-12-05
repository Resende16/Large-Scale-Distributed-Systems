package edu.sdle.crdt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sdle.User;
import edu.sdle.crdt.collections.AWMap;
import edu.sdle.crdt.lww.LWWRegister;

import java.util.UUID;

/// A shopping list, implemented as a CRDT.
public class ShoppingList implements ICRDT<ShoppingList> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonProperty private final UUID id;
    @JsonProperty private final LWWRegister<String> name;
    @JsonProperty private final AWMap<String, ItemQuantity> items;

    public ShoppingList(String name, User creator) {
        this.id = UUID.randomUUID();
        this.name = new LWWRegister<>(name, creator.id());
        this.items = new AWMap<>();
    }

    @JsonCreator
    private ShoppingList(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") LWWRegister<String> name,
            @JsonProperty("items") AWMap<String, ItemQuantity> items
    ) {
        this.id = id;
        this.name = name;
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
    public AWMap<String, ItemQuantity> getItems() {
        return items;
    }



    public boolean addItem(String itemName, int maxQuantity, UUID nodeId) {
        if (itemName == null || itemName.isBlank())

            return false;
        if (maxQuantity <= 0)
            return false;

        ItemQuantity existing = items.get(itemName);
        if (existing != null) {
            return false;
        }

        ItemQuantity qty = new ItemQuantity(maxQuantity);
        items.put(itemName, qty, nodeId);
        return true;
    }


    //TODO fix remove should have an UUID nodeId parameter
    public boolean removeItem(String itemName) {
        if (itemName == null || itemName.isBlank())
            return false;

        ItemQuantity existing = items.get(itemName);
        if (existing == null)
            return false;

        return items.remove(itemName);
    }


    public int incrementItem(String itemName, int amount, UUID nodeId) {
        if (itemName == null || itemName.isBlank())
            return -1;
        if (amount <= 0)
            return -1;

        ItemQuantity qty = items.get(itemName);
        if (qty == null)
            return -1;

        int i;

        for (i = 0; i < amount; i++) {
            if(!qty.inc(nodeId)){
                return i;
            }
        }
        return i;
    }



    public int decrementItem(String itemName, int amount, UUID nodeId) {
        if (itemName == null || itemName.isBlank())
            return -1;
        if (amount <= 0)
            return -1;

        ItemQuantity qty = items.get(itemName);
        if (qty == null)
            return -1;

        int i;
        for (i = 0; i < amount; i++) {
            if(!qty.dec(nodeId)){
                return i;
            }
            qty.dec(nodeId);
        }
        return i;
    }





    public void setName(String name, UUID node) {
        this.name.assign(name, node);
    }

    // Merge a shopping list into this one
    @Override
    public void merge(ShoppingList other) {
        name.merge(other.name);
        items.merge(other.items);
    }

    // Serialize a ShoppingList object to a Json string
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    // Deserialize a ShoppingList object from a Json string
    public static ShoppingList fromJson(String json) {
        try {
            return MAPPER.readValue(json, ShoppingList.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
