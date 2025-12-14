package edu.sdle.commands;


import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.crdt.ShoppingList;
import edu.sdle.storage.Storage;

import java.io.IOException;
import java.util.UUID;

public class DecCommand extends Command {


    public DecCommand() {
        super("/dec", "Command to Decrease the quantity of an item in a list. Usage: /dec <item_name> <dec_Value> <listId>");
    }

    @Override
    public void execute(String commandStr, Storage storage, User user) {
        String[] parts = commandStr.split(" ");
        if (parts.length < 4) {
            System.out.println("Usage: /dec <item_name> <dec_value> <list_id>");
            return;
        }

        String itemName = parts[1];
        int decValue = Integer.parseInt(parts[2]);
        UUID listId;
        try {
            listId = UUID.fromString(parts[3]);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: invalid <listId> format.");
            return;
        }


        ShoppingList list = storage.getObj(listId);
        if (list == null) {
            System.out.println("Error: shopping list not found: " + listId);
            return;
        }

        int done = list.decrementItem(itemName, decValue, user.id());

        if (done == -1) {
            System.out.println("Error: failed to decrease item quantity. Item may not exist or parameters are invalid.");
            return;
        }


        if (done < decValue) {
            System.out.println("Reached minimum quantity (0). Decreased only " + done);
        } else {
            System.out.println("Successfully decreased e" + itemName + " by" + done);
        }


        storage.putObj(list);
        System.out.println("[Storage] List saved.");


    }


}
