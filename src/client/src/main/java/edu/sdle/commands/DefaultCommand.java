package edu.sdle.commands;

import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.storage.Storage;

import java.util.UUID;

public class DefaultCommand extends Command {

    public DefaultCommand() {
        super("", "");
    }

    @Override
    public void execute(String commandStr, Storage storage, User user) {
        System.out.println("Unknown command! Use /help for more information.");
    }
}
