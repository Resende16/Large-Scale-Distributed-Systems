package edu.sdle.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sdle.crdt.ShoppingList;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class FileStorage implements Storage {

    private final String dirName;
    private final ObjectMapper mapper;

    public FileStorage() {
        // Use current directory
        this("./");
    }

    public FileStorage(String dirName) {
        if (!dirName.endsWith(File.separator))
            dirName = dirName + File.separator;

        this.dirName = dirName;
        this.mapper = new ObjectMapper();

        // Create directory if needed
        File dir = new File(dirName);
        if (!dir.exists())
            dir.mkdirs();
    }

    public boolean put(ShoppingList list) {
        try {
            File file = new File(dirName + list.getId() + ".json");
            mapper.writeValue(file, list);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean putJson(UUID key, String value) {
        File file = new File(dirName + key + ".json");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(value);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean remove(UUID id) {
        File file = new File(dirName + id + ".json");
        return file.exists() && file.delete();
    }

    public ShoppingList get(UUID id) {
        File file = new File(dirName + id + ".json");
        if (!file.exists())
            return null;

        try {
            return mapper.readValue(file, ShoppingList.class);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getAsJson(UUID id) {
        File file = new File(dirName + id + ".json");
        if (!file.exists())
            return null;

        try {
            return Files.readString(Path.of(file.getAbsolutePath()));
        } catch (IOException e) {
            return null;
        }
    }
}
