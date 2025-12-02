package edu.sdle.commands;
import edu.sdle.Command;

public class AddCommand extends Command {


    public AddCommand() {
        // Name is redundant here, it's the default command
        super("/add", "Command to add an item to a list. Usage: /add <item_name> <max_item> <listId>");
    }

    @Override
    public void execute( String commandStr) {
        System.out.println("Unknown command! Use /help for more information."
        );
    }

}
