package edu.sdle;

import edu.sdle.storage.FileStorage;
import edu.sdle.storage.Storage;

import java.io.File;
import java.util.Scanner;
import java.util.UUID;

public class Client {

    private final CommandHandler commandHandler;
    private final User user;

    public Client(String[] args) {
        String uidStr = null;
        String userName = "Steve";

        for (String arg : args) {
            if (arg.startsWith("--id=")) {
                uidStr = arg.substring("--id=".length());
            } else if (arg.startsWith("--name=")) {
                userName = arg.substring("--name=".length());
            }
        }

        UUID userId;
        if (uidStr == null) {
            userId = UUID.randomUUID();
            System.out.println("New user: " + userId);
        } else {
            userId = UUID.fromString(uidStr);
        }

        this.user = new User(userId, userName);

        String userDir = "." + File.separator + "storage" + File.separator + user.id();
        Storage storage = new FileStorage(userDir);
        this.commandHandler = new CommandHandler(storage, this.user);
        this.commandHandler.registerCommands();
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome: \n " + user.name());

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("quit")) {
                break;
            }
            if (!line.isEmpty()) {
                commandHandler.handleCommand(line);
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client(args);
        client.run();
    }
}
