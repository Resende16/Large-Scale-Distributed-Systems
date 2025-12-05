package edu.sdle.commands;

import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.storage.Storage;

import java.io.IOException;

public class CreateCommand extends Command {

    public CreateCommand() {
        super("/create", "Creates a new shopping list. Usage: /create <list_name>");
    }

    @Override
    public void execute(String commandStr, Storage storage, User user) {
        String[] parts = commandStr.split(" ", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            System.out.println("Usage: /create <list_name>");
            return;
        }

        String listName = parts[1].trim();

        try {
            ShoppingList list = new ShoppingList(listName, user);

            storage.put(list);

            System.out.println("Created list: \"" + list.getName() + "\" [" + list.getId() + "]");

        } catch (IOException e) {
            System.out.println("Error: failed to save list: " + e.getMessage());
        }
    }
}
