package edu.sdle.commands;


import edu.sdle.Command;

import java.util.Map;

import java.util.Map;

public class HelpCommand extends Command {
    private final Map<String, Command> commands;

    public HelpCommand(Map<String, Command> commands) {
        super("/help", "Show commands list");
        this.commands = commands;
    }

    @Override
    public void execute(String commandStr) {
        System.out.println("--------------------------------------");
        System.out.println("Here's the list of server commands:");

        // Versão com for-each clássico
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            Command cmd = entry.getValue();
            System.out.println(cmd.getName() + " - " + cmd.getDescription());
        }

        // OU, se preferires Java 8+ com lambda:
        // commands.forEach((key, cmd) ->
        //         System.out.println(cmd.getName() + " - " + cmd.getDescription())
        // );

        System.out.println("--------------------------------------");
    }
}
