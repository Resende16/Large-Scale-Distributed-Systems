package edu.sdle;

import java.util.ArrayList;
import java.util.List;

public class CommandRegistry {
    private static final List<Command> commands = new ArrayList<>();

    public static void register(Command command) {
        commands.add(command);
    }

    public static List<Command> getCommands() {
        return commands;
    }
}
