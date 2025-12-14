package edu.sdle.commands;

import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.storage.Storage;

import java.io.IOException;
import java.util.UUID;

public class DeleteCommand extends Command {

    public DeleteCommand() {
        super("/del", "Deletes a list by its ID. Usage: /delete <listId>");
    }

    @Override
    public void execute(String commandStr, Storage storage, User user) {
        String[] parts = commandStr.split(" ");
        if (parts.length < 2) {
            System.out.println("Usage: /delete <listId>");
            return;
        }

        UUID listId;
        try {
            listId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: invalid <listId> format.");
            return;
        }

        if (!storage.remove(listId)) {
            System.out.println("Error: list not found or could not be deleted: " + listId);
        } else {
            System.out.println("List deleted: " + listId);
        }
    }
}
