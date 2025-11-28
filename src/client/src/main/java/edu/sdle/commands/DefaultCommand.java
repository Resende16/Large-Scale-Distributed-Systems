package edu.sdle.commands;

import edu.sdle.Command;

public class DefaultCommand extends Command {

    public DefaultCommand() {
        // Name is redundant here, it's the default command
        super("", "");
    }

    @Override
    public void execute( String commandStr) {
        System.out.println("Unknown command! Use /help for more information."
        );
    }
}
