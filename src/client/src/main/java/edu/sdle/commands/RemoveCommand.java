package edu.sdle.commands;

import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.storage.Storage;
import java.io.IOException;
import java.util.UUID;

public class RemoveCommand extends Command {
    public RemoveCommand() {
        super("/remove", "Command to remove a new item of a  list. Usage: /remove <item_name> <listId>");
    }

    @Override
    public void execute(String commandStr, Storage storage, User user) {

        String[] parts = commandStr.split(" ");
        if (parts.length < 3) {
            System.out.println("Usage: /remove <item_name> <listId>");
            return;
        }

        String itemName = parts[1];


        UUID listId;
        try {
            listId = UUID.fromString(parts[2]);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: invalid <listId> format.");
            return;
        }

        ShoppingList list = storage.getObj(listId);
        if (list == null) {
            System.out.println("Error: shopping list not found: " + listId);
            return;
        }
        boolean success = list.removeItem(itemName);
        if (!success) {
            System.out.println("Error: failed to remove item. Item may not exist.");
            return;
        }
        storage.putObj(list);
        System.out.println("[Storage] List saved.");


    }


}


