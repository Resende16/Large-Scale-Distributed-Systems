package edu.sdle.storage;

import java.util.ArrayList;
import java.util.UUID;

public interface Storage {
    /**
     * Get a value from storage, as a Json string.
     * @param id Unique ID of the value
     * @return A Json string, or null on fail
     */
    String get(UUID id);

    /**
     * Get keys for all entries in storage.
     * @return An ArrayList of keys
     */
    ArrayList<String> getKeys();

    /**
     * Store a value in storage, provided its Json string.
     * @param value A ShoppingList object to store
     * @return True on success, false on fail
     */
    boolean put(UUID key, String value);

    /**
     * Delete an entry from storage.
     * @param id Key of the entry
     * @return True on success, false on fail
     */
    boolean remove(UUID id);
}
