package edu.sdle.commands;

import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.comms.CommsManager;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.storage.Storage;

import java.util.UUID;

public class PullCommand extends Command {

    private final CommsManager comms;

    public PullCommand(CommsManager comms) {
        super("/pull", "Download a shopping list from the cloud. Usage: /pull <list_id>");
        this.comms = comms;
    }

    @Override
    public void execute(String commandStr, Storage storage, User user) {
        String[] parts = commandStr.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Usage: /pull <list_id>");
            return;
        }

        try {
            UUID id = UUID.fromString(parts[1]);
            ShoppingList remote = comms.getObj(id);
            if (remote == null) {
                System.out.println("Cloud does not have list " + id + " or request failed.");
                return;
            }

            ShoppingList local = storage.getObj(id);

            if (local == null) {
                storage.putObj(remote);
                System.out.println("Pulled list " + id + " (no local version existed).");
            } else {
                local.merge(remote);
                storage.putObj(local);
                System.out.println("Pulled and merged list " + id + " with local version.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid UUID. Usage: /pull <list_id>");
        }
    }

}
