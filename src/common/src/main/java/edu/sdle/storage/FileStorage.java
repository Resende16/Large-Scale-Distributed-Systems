package edu.sdle.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sdle.crdt.ShoppingList;

import java.io.*;
import java.util.ArrayList;
import java.util.UUID;

public class FileStorage implements Storage {

    private final String dirName;
    private final ObjectMapper mapper;

    public FileStorage() {
        // Use current directory
        this("./");
    }

    public FileStorage(String dirName) {
        this.dirName = dirName;
        this.mapper = new ObjectMapper();

        // Create directory if needed
        File dir = new File(dirName);
        if (!dir.exists())
            dir.mkdir();
    }

    public boolean add(ShoppingList list) throws IOException {
        File file = new File(dirName + list.getId().toString() + ".json");
        mapper.writeValue(file, list);
        return true;
    }

    public boolean remove(UUID id) {
        File file = new File(dirName + id.toString() + ".json");
        return file.exists() && file.delete();
    }

    public ShoppingList get(UUID id) throws IOException {
        File file = new File(dirName + id.toString() +  ".json");
        if (!file.exists())
            return null;

        return mapper.readValue(file, ShoppingList.class);
    }

    public ArrayList<ShoppingList> getAll() throws IOException {
        ArrayList<ShoppingList> result = new ArrayList<>();

        File dir = new File(dirName);
        if (!dir.exists())
            return result;

        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                // Filter for JSON files
                if (file.getName().endsWith(".json"))
                    result.add(mapper.readValue(file, ShoppingList.class));
            }
        }
        return result;
    }
}
