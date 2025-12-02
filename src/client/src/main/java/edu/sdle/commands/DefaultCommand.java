package edu.sdle.commands;

import edu.sdle.Command;
import edu.sdle.storage.FileStorage;

import java.util.UUID;

public class DefaultCommand extends Command {

    public DefaultCommand() {
        // Name is redundant here, it's the default command
        super("", "");
    }

    @Override
    public void execute(String commandStr, FileStorage userStorage, UUID userId) {
        System.out.println("Unknown command! Use /help for more information."
        );
    }
}
