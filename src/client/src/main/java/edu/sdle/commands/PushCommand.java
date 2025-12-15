package edu.sdle.commands;

import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.comms.CommsManager;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.storage.Storage;

import java.util.UUID;

public class PushCommand extends Command {

    private final CommsManager comms;

    public PushCommand(CommsManager comms) {
        super("/push", "Upload a local shopping list to the cloud. Usage: /push <list_id>");
        this.comms = comms;
    }

    @Override
    public void execute(String commandStr, Storage storage, User user) {
        String[] parts = commandStr.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Usage: /push <list_id>");
            return;
        }

        try {
            UUID id = UUID.fromString(parts[1]);
            ShoppingList list = storage.getObj(id);
            if (list == null) {
                System.out.println("No local list with id " + id);
                return;
            }

            boolean ok = comms.put(list);
            if (ok) {
                System.out.println("Pushed list " + id + " to cloud.");
            } else {
                System.out.println("Failed to push list " + id + " to cloud.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid UUID. Usage: /push <list_id>");
        }
    }
}
