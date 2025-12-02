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
        this("./");
    }

    public FileStorage(String dirName) {
        if (!dirName.endsWith(File.separator)) {
            dirName = dirName + File.separator;
        }
        this.dirName = dirName;
        this.mapper = new ObjectMapper();

        File dir = new File(this.dirName);
        if (!dir.exists()) {
            boolean ok = dir.mkdirs();
            if (!ok) {
                throw new RuntimeException("Failed to create storage directory: " + this.dirName);
            }
        }
    }


    public boolean put(ShoppingList list) throws IOException {
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
