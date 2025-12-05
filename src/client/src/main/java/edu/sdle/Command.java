package edu.sdle;

import edu.sdle.storage.FileStorage;
import edu.sdle.storage.Storage;

import java.util.UUID;

public abstract class Command {

    private final String name;
    private final String description;

    public Command(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return  description;
    }

    public abstract void execute(String commandStr, Storage Storage, User user);
}
