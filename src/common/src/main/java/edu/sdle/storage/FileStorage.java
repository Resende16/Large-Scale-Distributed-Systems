package edu.sdle.storage;

import edu.sdle.JsonUtils;
import edu.sdle.crdt.ShoppingList;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
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

    private boolean putObj(ShoppingList obj) {
        var file = new File(dir, obj.getId() + ".json");
        return JsonUtils.toJson(obj, file);
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

    public ArrayList<String> getKeys() {
        ArrayList<String> keys = new ArrayList<>();
        var files = dir.listFiles((_, name) -> name.endsWith(".json"));

        if (files != null) {
            for (File file : files) {
                var k = file.getName().replace(".json", "");
                keys.add(k);
            }
        }

        return keys;
    }

    public boolean put(UUID id, String value) {
        var thisObj = JsonUtils.fromJson(value, ShoppingList.class);
        if (thisObj == null)
            return false;

        var currObj = getObj(id);

        if (currObj != null) {
            // Merge first, then put
            currObj.merge(thisObj);
            return putObj(currObj);
        } else
            return putObj(thisObj);
    }

    public boolean remove(UUID id) {
        File file = new File(dir, id + ".json");
        return file.exists() && file.delete();
    }
}
