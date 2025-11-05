package edu.sdle.crdt;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LWWRegister.class, name = "LWWRegister"),
        @JsonSubTypes.Type(value = GCounter.class, name = "GCounter"),
        @JsonSubTypes.Type(value = PNCounter.class, name = "PNCounter"),
        @JsonSubTypes.Type(value = GSet.class, name = "GSet"),
        @JsonSubTypes.Type(value = AWSet.class, name = "AWSet"),
        @JsonSubTypes.Type(value = AWMap.class, name = "AWMap"),
        @JsonSubTypes.Type(value = ItemQuantity.class, name = "ItemQuantity"),
        @JsonSubTypes.Type(value = ShoppingList.class, name = "ShoppingList")
})
/// A conflict-free replicated data type
public interface ICRDT<T extends ICRDT<T>> {
    /**
     * Merge another CRDT into this one; types must match
     * @param other CRDT to merge with
     */
    void merge(T other);
}
