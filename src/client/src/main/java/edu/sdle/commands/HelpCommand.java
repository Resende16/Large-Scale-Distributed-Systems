package edu.sdle.commands;


import edu.sdle.Command;
import edu.sdle.User;
import edu.sdle.storage.FileStorage;

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
    public void execute(String commandStr, FileStorage userStorage, User user) {
        System.out.println("--------------------------------------");
        System.out.println("Here's the list of server commands:");

        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            Command cmd = entry.getValue();
            System.out.println(cmd.getName() + " - " + cmd.getDescription());
        }

        System.out.println("--------------------------------------");
    }
}
