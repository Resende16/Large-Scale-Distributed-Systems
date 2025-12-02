package edu.sdle.commands;


import edu.sdle.Command;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.storage.FileStorage;

import java.io.IOException;
import java.util.UUID;

public class IncCommand extends Command {


    public IncCommand() {
        super("/inc", "Command to increase the quantity of an item in a list. Usage: /inc <item_name> <inc_Value> <listId>");
    }

    @Override
    public void execute(String commandStr, FileStorage userStorage, UUID userId) {
        String[] parts = commandStr.split(" ");
        if (parts.length < 4) {
            System.out.println("Usage: /inc <item_name> <inc_Value> <listId>");
            return;
        }
        String itemName = parts[1];
        int incValue = Integer.parseInt(parts[2]);
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

            int done = list.incrementItem(itemName, incValue, userId);

            if (done == -1) {
                System.out.println("Error: failed to increase item quantity. Item may not exist or parameters are invalid.");
                return;
            }


            if (done < incValue) {
                System.out.println("Reached max quantity. Increased only " + done);
            } else {
                System.out.println("Successfully increased " + itemName + " by " + done);
            }

            userStorage.put(list);
            System.out.println("[Storage] List saved.");

        } catch (IOException e) {
            System.out.println("Error: failed to access storage: " + e.getMessage());
        }
    }


}
