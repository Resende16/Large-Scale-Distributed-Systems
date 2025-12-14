package edu.sdle.commands;

import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.crdt.ItemQuantity;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.crdt.collections.AWMap;
import edu.sdle.storage.Storage;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ShowCommand extends Command {

    public ShowCommand() {
        super("/show", "Shows a shopping list or all lists. Usage: /show <listId> or /show");
    }

    @Override
    public void execute(String commandStr, Storage storage, User user) {
        String[] parts = commandStr.split(" ");

        if (parts.length == 1) {
            handleShowAll(storage);
            return;
        }

        if (parts[1].equalsIgnoreCase("all")) {
            handleShowAll(storage);
            return;
        }

        handleShowOne(parts[1], storage);
    }


    private void handleShowAll(Storage storage) {

        var lists = storage.getKeys();
        if (lists.isEmpty()) {
            System.out.println("No shopping lists found.");
            return;
        }

        System.out.println("Shopping lists:");
        for (var list : lists) {
            System.out.println("  - " + list + "]");
        }

    }

    private void handleShowOne(String idStr, Storage storage) {
        UUID listId;
        try {
            listId = UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: invalid <listId> format.");
            return;
        }


        ShoppingList list = storage.getObj(listId);
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


    }
}

