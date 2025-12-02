package edu.sdle.commands;

import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.crdt.ItemQuantity;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.crdt.collections.AWMap;
import edu.sdle.storage.FileStorage;

import java.io.IOException;
import java.util.UUID;

public class ShowCommand extends Command {

    public ShowCommand() {
        super("/show", "Command to show the contents of a shopping list. Usage: /show <listId>");
    }

    @Override
    public void execute(String commandStr, FileStorage userStorage, User user) {
        String[] parts = commandStr.split(" ");
        if (parts.length < 2) {
            System.out.println("Usage: /show <listId>");
            return;
        }

        UUID listId;
        try {
            listId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: invalid <listId> format.");
            return;
        }

        try {
            ShoppingList list = userStorage.get(listId);
            if (list == null) {
                System.out.println("Error: shopping list not found: " + listId);
                return;
            }

            System.out.println("List: " + list.getName() + " [" + list.getId() + "]");

            AWMap<String, ItemQuantity> items = list.getItems();
            if (items.entrySet().isEmpty()) {
                System.out.println("  (no items)");
                return;
            }

            for (var entry : items.entrySet()) {
                String itemName = entry.key();
                ItemQuantity qty = entry.value();
                System.out.println("  - " + itemName + ": " +
                        qty.getCurrent() + "/" + qty.getMax());
            }

        } catch (IOException e) {
            System.out.println("Error: failed to access storage: " + e.getMessage());
        }
    }
}
