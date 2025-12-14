package edu.sdle;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SeedNodeRegistry {

    private static final String FILENAME = "seed-nodes.txt";
    private final List<String> seedAddresses;

    // for Singleton
    private static final class Holder {
        private static final SeedNodeRegistry INSTANCE = new SeedNodeRegistry();
    }

    private SeedNodeRegistry() {
        this.seedAddresses = loadFromFile();
    }

    public static SeedNodeRegistry getInstance() {
        return Holder.INSTANCE;
    }

    public List<String> getSeedAddresses() {
        return seedAddresses;
    }

    private List<String> loadFromFile() {
        InputStream in = SeedNodeRegistry.class
                .getClassLoader()
                .getResourceAsStream(FILENAME);

        if (in == null) {
            throw new IllegalStateException(
                    "Set of seed nodes not found in src/main/resources"
            );
        }

        List<String> seedNodes = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))
                .lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .peek(line -> {
                    if (!line.contains(":")) {
                        throw new IllegalArgumentException(
                                "Invalid seed node address '"+ line + "', missing colon"
                        );
                    }
                })
                .toList();

        if (seedNodes.isEmpty()) {
            throw new IllegalStateException("Set of known seed nodes is empty");
        }

        return List.copyOf(seedNodes);
    }
}
