package edu.sdle.commands;


import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.storage.FileStorage;
import edu.sdle.storage.Storage;

import java.util.Map;

import java.util.Map;
import java.util.UUID;

public class HelpCommand extends Command {
    private final Map<String, Command> commands;

    public HelpCommand(Map<String, Command> commands) {
        super("/help", "Show commands list");
        this.commands = commands;
    }

    @Override
    public void execute(String commandStr, Storage storage, User user) {
        System.out.println("--------------------------------------");
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            Command cmd = entry.getValue();
            System.out.println(cmd.getName() + " - " + cmd.getDescription());
        }

        System.out.println("--------------------------------------");
    }
}
