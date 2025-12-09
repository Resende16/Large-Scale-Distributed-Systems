package edu.sdle;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ProxyRegistry {

    private static final String FILENAME = "proxy-nodes.txt";
    private final List<String> proxyAddresses;

    // for Singleton
    private static final class Holder {
        private static final ProxyRegistry INSTANCE = new ProxyRegistry();
    }

    private ProxyRegistry() {
        this.proxyAddresses = loadFromFile();
    }

    public static ProxyRegistry getInstance() {
        return Holder.INSTANCE;
    }

    public List<String> getProxyAddresses() {
        return proxyAddresses;
    }

    private List<String> loadFromFile() {
        InputStream in = ProxyRegistry.class
                .getClassLoader()
                .getResourceAsStream(FILENAME);

        if (in == null) {
            throw new IllegalStateException(
                    "Set of proxies not found in src/main/resources"
            );
        }

        List<String> proxies = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))
                .lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .peek(line -> {
                    if (!line.contains(":")) {
                        throw new IllegalArgumentException(
                                "Invalid proxy address '"+ line + "', missing colon"
                        );
                    }
                })
                .toList();

        if (proxies.isEmpty()) {
            throw new IllegalStateException("Set of known proxies is empty");
        }

        return List.copyOf(proxies);
    }
}