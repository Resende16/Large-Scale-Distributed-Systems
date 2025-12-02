package edu.sdle.commands;

import edu.sdle.Command;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.storage.FileStorage;

import java.io.IOException;
import java.util.UUID;

//ncurses fazem
public class RemoveCommand extends Command {
    public RemoveCommand() {
        super("/remove", "Command to remove a new item of a  list. Usage: /remove <item_name> <listId>");
    }

    @Override
    public void execute(String commandStr, FileStorage userStorage, UUID userId) {

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
        try {
            ShoppingList list = userStorage.get(listId);
            if (list == null) {
                System.out.println("Error: shopping list not found: " + listId);
                return;
            }
            boolean success = list.removeItem(itemName);
            if (!success) {
                System.out.println("Error: failed to remove item. Item may not exist.");
                return;
            }
            userStorage.put(list);
            System.out.println("[Storage] List saved.");

        } catch (IOException e) {
            System.out.println("§cError: failed to access storage: " + e.getMessage());
        }
    }


}


