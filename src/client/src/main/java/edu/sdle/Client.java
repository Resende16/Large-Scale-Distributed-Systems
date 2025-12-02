package edu.sdle;

import edu.sdle.storage.FileStorage;

import java.util.Scanner;
import java.util.UUID;

public class Client {

    private final FileStorage storage;
    private final CommandHandler commandHandler;
    private final UUID userId;

    public Client(String[] args) {
        String uidStr = null;
        for (String arg : args) {
            if (arg.startsWith("--uid=")) {
                uidStr = arg.substring("--uid=".length());
            }
        }

        if (uidStr == null) {
            this.userId = UUID.randomUUID();
            System.out.println("Novo UID gerado: " + userId);
        } else {
            this.userId = UUID.fromString(uidStr);
        }

        String userDir = "./storage/" + userId + "/";
        this.storage = new FileStorage(userDir);

        this.commandHandler = new CommandHandler();
        this.commandHandler.registerCommands();
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome. UID: " + userId);

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("quit") ) {
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
