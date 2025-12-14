package edu.sdle.storage;

import edu.sdle.utils.JsonUtils;
import edu.sdle.crdt.ShoppingList;

import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FileStorage implements Storage {
    private final File dir;

    public FileStorage() {
        // Use current directory
        this("./");
    }

    public FileStorage(String dirName) {
        if (!dirName.endsWith(File.separator))
            dirName = dirName + File.separator;

        // Create directory if needed
        File dir = new File(dirName);
        if (!dir.exists())
            dir.mkdirs();

        this.dir = dir;
    }

    public ShoppingList getObj(UUID id) {
        var file = new File(dir, id + ".json");
        if (!file.exists()) {
            return null;
        }

        return JsonUtils.fromJson(file, ShoppingList.class);
    }

    public String get(UUID id) {
        File file = new File(dir, id + ".json");
        if (!file.exists()) {
            return null;
        }

        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            return null;
        }
    }

    public Set<UUID> getKeys() {
        Set<UUID> keys = new HashSet<>();
        var files = dir.listFiles((_, name) -> name.endsWith(".json"));

        if (files != null) {
            for (File file : files) {
                var k = file.getName().replace(".json", "");
                try {
                    keys.add(UUID.fromString(k)); // convert key String to UUID
                } catch (IllegalArgumentException _) {}
            }
        }

        return keys;
    }

    public boolean put(UUID id, String value) {
        var thisObj = JsonUtils.fromJson(value, ShoppingList.class);
        if (thisObj == null)
            return false;

        return putObj(thisObj);
    }

    // private utility does not merge
    private boolean _putObj(ShoppingList obj) {
        var file = new File(dir, obj.getId() + ".json");
        return JsonUtils.toJson(obj, file);
    }

    // public method does merge
    public boolean putObj(ShoppingList obj) {
        var currObj = getObj(obj.getId());

        if (currObj != null) {
            // Merge first, then put
            currObj.merge(obj);
            return _putObj(currObj);
        } else
            return _putObj(obj);
    }

    public boolean remove(UUID id) {
        File file = new File(dir, id + ".json");
        return file.exists() && file.delete();
    }
}
