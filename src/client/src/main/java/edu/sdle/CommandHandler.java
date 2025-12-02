package edu.sdle;
import edu.sdle.commands.DefaultCommand;
import edu.sdle.commands.HelpCommand;
import edu.sdle.storage.FileStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CommandHandler {

    private final Map<String, Command> commands;
    private Command defaultCommand;
    private final Lock lock;
    private final UUID userId;
    private final FileStorage userStorage;

    public CommandHandler(FileStorage userStorage, UUID userId) {
        this.userStorage= userStorage;
        this.userId= userId;
        this.commands = new HashMap<>();
        this.lock = new ReentrantLock();
    }

    // Registers all server commands
    public void registerCommands() {

        setDefaultCommand(new DefaultCommand());
        registerCommand(new HelpCommand(this.commands));
    }

    public boolean registerCommand(Command command) {
        if (command == null)
            return false;

        lock.lock();
        try {
            String commandName = command.getName();
            if (commands.containsKey(commandName)) {
                // Command already registered
                return false;
            }

            commands.put(commandName, command);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void setDefaultCommand(Command defaultCommand) {
        lock.lock();
        try {
            this.defaultCommand = defaultCommand;
        } finally {
            lock.unlock();
        }
    }

    public void handleCommand(String commandStr) {

        String commandName = commandStr.split(" ")[0];
        lock.lock();
        try {
            if (commands.containsKey(commandName)) {
                Command command = commands.get(commandName);
                command.execute(commandStr,this.userStorage);
            }
            else if (defaultCommand != null) {
                defaultCommand.execute(commandStr,this.userStorage);
            }
        } finally {
            lock.unlock();
        }
    }
}
