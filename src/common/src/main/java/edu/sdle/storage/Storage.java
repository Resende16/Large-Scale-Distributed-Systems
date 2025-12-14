package edu.sdle.storage;

import edu.sdle.crdt.ShoppingList;

import java.util.Set;
import java.util.UUID;

public interface Storage {
    /**
     * Get a value from storage, as a Json string.
     * @param id Unique ID of the value
     * @return A Json string, or null on fail
     */
    String get(UUID id);

    /**
     * Get a Shopping List from storage.
     * @param id Unique ID of the value
     * @return A shopping list, or null on fail
     */
    ShoppingList getObj(UUID id);

    /**
     * Get keys for all entries in storage.
     * @return A Set of keys
     */
    Set<UUID> getKeys();

    /**
     * Store an entry in storage, provided its Json string.
     * @param key Key of the entry
     * @param value A Json string to store
     * @return True on success, false on fail
     */
    boolean put(UUID key, String value);

    /**
     * Store a Shopping List in storage.
     * @param obj A ShoppingList to store
     * @return True on success, false on fail
     */
    boolean putObj(ShoppingList obj);

    /**
     * Delete an entry from storage.
     * @param id Key of the entry
     * @return True on success, false on fail
     */
    boolean remove(UUID id);
}
