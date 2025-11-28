package edu.sdle;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.



import edu.sdle.crdt.ShoppingList;

import java.util.Scanner;
import java.util.UUID;


public class Client {

    private static final Scanner scanner = new Scanner(System.in);

    private final UUID nodeId = UUID.randomUUID();

    private ShoppingList currentList;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    // Main loop
    private void run() {
        System.out.println("Welcome to the Shopping List client!");

        while (true) {
            showMainMenu();
            String choice = readLine();
            //List minecraf based.

            switch (choice) {
                case "1":
                    createNewList();
                    break;
                case "2":
                    openListById();
                    break;
                case "3":
                    showCurrentList();
                    break;
                case "4":
                    manageItemsMenu();
                    break;
                case "5":
                    saveCurrentList();
                    break;
                case "6":
                    mergeWithOtherVersion();
                    break;
                case "0":
                    System.out.println("Bye!");
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    // ----- UI helpers -----

    private void showMainMenu() {
        System.out.println();
        System.out.println("§8==============================");
        System.out.println("§a   ShoppingList » Main Menu");
        System.out.println("§8==============================");
        System.out.println("§e 1§7) Create new list");
        System.out.println("§e 2§7) Open list by ID");
        System.out.println("§e 3§7) Show current list");
        System.out.println("§e 4§7) Manage items");
        System.out.println("§e 5§7) Save current list");
        System.out.println("§e 6§7) Sync / merge");
        System.out.println("§e 0§7) Exit");
    }


    private String readLine() {
        System.out.print("> ");
        return scanner.nextLine().trim();
    }

    // ----- Operations to implement next -----

    // Use new ShoppingList(name, user) with your User/creator
    private void createNewList() {
        // TODO: ask for list name, create ShoppingList, keep in currentList
        // criar a nova instancia de list e retomar um id de list para ser shared
        // and print the list ID so it can be shared
    }

    private void openListById() {
        // TODO: ask for UUID string, load JSON from disk/cloud, set currentList
        //abrir a list e o current fica setado
    }

    private void showCurrentList() {
        // TODO: print list name, ID, and all items with current/max quantities
    }

    private void manageItemsMenu() {
        if (currentList == null) {
            System.out.println("No list opened.");
            return;
        }
        // TODO: sub‑menu: add item, remove item, inc/dec quantity
    }

    private void saveCurrentList() {
        // TODO: serialize currentList.toJson() to a file (local persistence)
    }

    private void mergeWithOtherVersion() {
        // TODO: load another ShoppingList (e.g. from another file/cloud),
        // call currentList.merge(other), then save
    }
}
