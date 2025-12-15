package edu.sdle;

import edu.sdle.commands.*;
import edu.sdle.comms.CommsManager;
import edu.sdle.storage.Storage;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler {

    private final Map<String, Command> commands;
    private Command defaultCommand;
    private final User user;
    private final Storage storage;
    private final CommsManager comms;

    public CommandHandler(Storage storage, User user,int timeoutPerProxy) {
        this.storage = storage;
        this.user = user;
        this.commands = new HashMap<>();
        this.comms= new CommsManager(timeoutPerProxy);
    }

    // Registers all server commands
    public void registerCommands() {

        setDefaultCommand(new DefaultCommand());
        registerCommand(new HelpCommand(this.commands));
        registerCommand(new AddCommand());
        registerCommand(new RemoveCommand());
        registerCommand(new DefaultCommand());
        registerCommand(new IncCommand());
        registerCommand(new DecCommand());
        registerCommand(new ShowCommand());
        registerCommand(new CreateCommand());
        registerCommand(new DeleteCommand());
        registerCommand(new PullCommand(this.comms));
        registerCommand(new PushCommand(this.comms));
    }

    public boolean registerCommand(Command command) {
        if (command == null)
            return false;

        String commandName = command.getName();

        if (commands.containsKey(commandName))
            // Command already registered
            return false;

        commands.put(commandName, command);
        return true;
    }

    public void setDefaultCommand(Command defaultCommand) {
        this.defaultCommand = defaultCommand;
    }

    public void handleCommand(String commandStr) {

        String commandName = commandStr.split(" ")[0];

        if (commands.containsKey(commandName)) {
            Command command = commands.get(commandName);
            command.execute(commandStr,this.storage, this.user);
        }
        else if (defaultCommand != null) {
            defaultCommand.execute(commandStr,this.storage, this.user);
        }
    }
}
