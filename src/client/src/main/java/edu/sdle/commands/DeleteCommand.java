package edu.sdle.commands;

import edu.sdle.Command;
import edu.sdle.storage.FileStorage;

import java.util.UUID;

public class DeleteCommand extends Command {

    public DeleteCommand() {
        super("/del", "Deletes a list by its ID. Usage: /delete <listId>");
    }

    @Override
    public void execute(String commandStr, FileStorage userStorage, UUID userId) {
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

        boolean removed = userStorage.remove(listId);
        if (!removed) {
            System.out.println("Error: list not found or could not be deleted: " + listId);
        } else {
            System.out.println("List deleted: " + listId);
        }
    }
}
