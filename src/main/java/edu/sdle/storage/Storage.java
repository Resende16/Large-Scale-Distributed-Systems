package edu.sdle.storage;

import edu.sdle.crdt.ShoppingList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public interface Storage {
    /**
     * Retrieve a shopping list from storage.
     * @param id Unique id of the list
     * @return A shopping list, or null if not found
     */
    ShoppingList get(UUID id) throws IOException;

    /**
     * Retrieve all shopping lists from storage.
     * @return An array of shopping lists
     */
    ArrayList<ShoppingList> getAll() throws IOException;

    /**
     * Add or update a shopping list.
     * @param list Shopping list to add
     * @return True on success, false otherwise
     */
    boolean add(ShoppingList list) throws IOException;

    /**
     * Remove a shopping list from storage.
     * @param id Unique id of the list
     * @return True on success, false otherwise
     */
    boolean remove(UUID id) throws IOException;
}
