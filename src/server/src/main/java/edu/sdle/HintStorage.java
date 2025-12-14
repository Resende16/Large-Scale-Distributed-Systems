package edu.sdle;

import edu.sdle.storage.FileStorage;
import edu.sdle.storage.Storage;

import java.io.*;
import java.util.*;

public class HintStorage {
    private final Storage hintStorage;

    private final File indexFile;
    private final Map<UUID, Set<UUID>> index = new HashMap<>();

    public HintStorage(String path) {
        this.hintStorage = new FileStorage(path);

        String indexPath = path + File.separator + "index.txt";
        indexFile = new File(indexPath);

        if (!indexFile.exists()) {
            try {
                indexFile.createNewFile();
            } catch (IOException _) {
                System.err.println("Failed to create index file for hint storage!");;
            }
        }

        loadIndex();
    }

    public synchronized void storeHint(UUID nodeId, UUID key, String value) {
        hintStorage.put(key, value);

        index.computeIfAbsent(nodeId, _ -> new HashSet<>()).add(key);
        saveIndex();
    }

    public synchronized Map<UUID, String> getHintsForNode(UUID nodeId) {
        Set<UUID> keys = index.get(nodeId);
        if (keys == null) {
            return Map.of();
        }

        Map<UUID, String> result = new HashMap<>();
        for (UUID key : keys) {
            String value = hintStorage.get(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    public synchronized void removeHintForNode(UUID nodeId, UUID key) {
        Set<UUID> keys = index.get(nodeId);
        if (keys == null) return;

        keys.remove(key);
        hintStorage.remove(key);

        if (keys.isEmpty()) {
            index.remove(nodeId);
        }

        saveIndex();
    }

    public synchronized void removeHintsForNode(UUID nodeId) {
        Set<UUID> keys = index.remove(nodeId);
        if (keys == null) return;

        for (UUID key : keys) {
            hintStorage.remove(key);
        }

        saveIndex();
    }

    public void loadIndex() {
        index.clear();
        if (!indexFile.exists())
            return;

        try (BufferedReader reader = new BufferedReader(new FileReader(indexFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                var parts = line.split(":", 2);
                if (parts.length != 2) continue;

                UUID nodeId = UUID.fromString(parts[0]);
                UUID key = UUID.fromString(parts[1]);

                index.computeIfAbsent(nodeId, _ -> new HashSet<>()).add(key);
            }

        } catch (IOException | IllegalArgumentException _) {
            //todo: Better logging
            System.err.println("Failed to load hint index!");
        }
    }

    private synchronized void saveIndex() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile))) {
            for (Map.Entry<UUID, Set<UUID>> entry : index.entrySet()) {
                UUID nodeId = entry.getKey();

                for (UUID key : entry.getValue()) {
                    writer.write(nodeId.toString() + ":" + key.toString());
                    writer.newLine();
                }
            }

        } catch (IOException _) {
            //todo: Better logging
            System.err.println("Failed to save hint index!");
        }
    }
}
