package edu.sdle.commands;


import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.storage.FileStorage;
import edu.sdle.storage.Storage;

import java.io.IOException;
import java.util.UUID;

public class IncCommand extends Command {


    public IncCommand() {
        super("/inc", "Command to increase the quantity of an item in a list. Usage: /inc <item_name> <inc_Value> <listId>");
    }

    @Override
    public void execute(String commandStr, Storage storage, User user) {
        String[] args = commandStr.split(" ");
        if (args.length < 4) {
            System.out.println("Usage: /inc <item_name> <inc_Value> <listId>");
            return;
        }
        String itemName = args[1];
        int incValue = Integer.parseInt(args[2]);
        UUID listId;
        try {
            listId = UUID.fromString(args[3]);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: invalid <listId> format.");
            return;
        }

        try {
            ShoppingList list = storage.get(listId);
            if (list == null) {
                System.out.println("Error: shopping list not found: " + listId);
                return;
            }

            int done = list.incrementItem(itemName, incValue, user.id());

            if (done == -1) {
                System.out.println("Error: failed to increase item quantity. Item may not exist or parameters are invalid.");
                return;
            }


            if (done < incValue) {
                System.out.println("Reached max quantity. Increased only " + done);
            } else {
                System.out.println("Successfully increased " + itemName + " by " + done);
            }

            storage.put(list);
            System.out.println("[Storage] List saved.");

        } catch (IOException e) {
            System.out.println("Error: failed to access storage: " + e.getMessage());
        }
    }


}
