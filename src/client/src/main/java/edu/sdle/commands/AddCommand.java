package edu.sdle.commands;
import edu.sdle.Command;
import edu.sdle.crdt.ItemQuantity;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.storage.FileStorage;

import java.io.IOException;
import java.util.UUID;

public class AddCommand extends Command {


    public AddCommand() {
        // Name is redundant here, it's the default command
        super("/add", "Command to add a new item to a list. Usage: /add <item_name> <max_item> <listId>");
    }


    @Override
    public void execute(String commandStr, FileStorage userStorage,UUID userId) {

        String[] parts = commandStr.split(" ");
        if (parts.length < 4) {
            System.out.println("Usage: /add <item_name> <max_item> <listId>");
            return;
        }

        String itemName = parts[1];
        int max;
        try {
            max = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            System.out.println("Error: <max_item> must be an integer.");
            return;
        }

        UUID listId;
        try {
            listId = UUID.fromString(parts[3]);
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
            boolean success = list.addItem(itemName, max, userId);
            if (!success) {
                System.out.println("Error: failed to add item. Item may already exist or invalid parameters.");
                return;
            }

            userStorage.put(list);
            System.out.println("[Storage] List saved.");

        } catch (IOException e) {
            System.out.println("§cError: failed to access storage: " + e.getMessage());
        }
    }


}
