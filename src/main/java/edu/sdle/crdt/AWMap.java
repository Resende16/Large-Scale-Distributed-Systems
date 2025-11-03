package edu.sdle.crdt;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/// An Add-Wins CRDT map; value type must be a CRDT
@JsonSerialize(using = AWMap.Serializer.class)
@JsonDeserialize(using = AWMap.Deserializer.class)
public class AWMap<K,V extends ICRDT<V>> implements ICRDT<AWMap<K, V>> {

    private final AWSet<AWMapEntry<K, V>> awSet = new AWSet<>();

    // Get the value associated with a key
    public V get(K key) {
        for (var e : awSet.value()) {
            if (e.key().equals(key)) {
                return e.value();
            }
        }
        return null;
    }

    // Map a key to a value in the map
    public void put(K key, V value, String nodeId) {
        var entry = new AWMapEntry<>(key, value);
        awSet.add(entry, nodeId);
    }

    // Check if the map contains a key
    public boolean contains(K key) {
        return awSet.contains(new AWMapEntry<>(key));
    }

    // Remove the mapping for a key from the map
    public boolean remove(K key) {
        return awSet.remove(new AWMapEntry<>(key));
    }

    public Set<AWMapEntry<K,V>> entrySet() {
        return awSet.value();
    }

    // Merge a AWMap with this one
    @Override
    public void merge(AWMap<K, V> other) {
        // Merge key set first
        awSet.merge(other.awSet);

        // Merge values
        for (var otherEntry : other.awSet.value()) {
            var entry = this.get(otherEntry.key());
            if (entry != null)
                entry.merge(otherEntry.value());
        }
    }

    @Override
    public String toString() {
        if (awSet.value().isEmpty())
            return "AWMap({})";

        StringBuilder sb = new StringBuilder("AWMap({\n");
        for (var e : awSet.value()) {
            sb.append("  ")
                    .append(e.key())
                    .append("=").append(e.value())
                    .append("\n");
        }
        sb.append("})");
        return sb.toString();
    }

    public static class Serializer<K, V extends ICRDT<V>> extends JsonSerializer<AWMap<K, V>> {
        @Override
        public void serialize(AWMap<K, V> map, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeFieldName("items");
            gen.writeStartArray();

            for (var e : map.entrySet()) {
                gen.writeStartObject();
                gen.writeObjectField("key", e.key());
                gen.writeObjectField("value", e.value());
                gen.writeObjectField("dots", map.awSet.items.get(e));
                gen.writeEndObject();
            }
            gen.writeEndArray();
            gen.writeObjectField("version", map.awSet.versionDots);
        }

        @Override
        public void serializeWithType(
                AWMap<K, V> map,
                JsonGenerator gen,
                SerializerProvider serializers,
                TypeSerializer typeSer
        ) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("type", "AWMap");
            serialize(map, gen, serializers);
            gen.writeEndObject();
        }
    }

    public static class Deserializer<K, V extends ICRDT<V>> extends JsonDeserializer<AWMap<K, V>> {
        @Override
        @SuppressWarnings("unchecked")
        public AWMap<K, V> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectCodec codec = p.getCodec();
            JsonNode root = codec.readTree(p);
            ObjectMapper mapper = (ObjectMapper) p.getCodec();

            AWMap<K, V> map = new AWMap<>();

            // Parse items
            JsonNode items = root.get("items");
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    K key = mapper.convertValue(item.get("key"), new TypeReference<>() {});
                    V value = (V) mapper.convertValue(item.get("value"), ICRDT.class);
                    Map<String, Integer> dots = mapper.convertValue(item.get("dots"),
                            new TypeReference<>() {});

                    var entry = new AWMapEntry<>(key, value);
                    map.awSet.items.put(entry, dots);
                }
            }

            // Parse version dots
            JsonNode version = root.get("version");
            if (version != null && version.isObject()) {
                Map<String, Integer> versionDots =
                        ((ObjectMapper) codec).convertValue(version, new TypeReference<>() {});
                map.awSet.versionDots.putAll(versionDots);
            }

            return map;
        }
    }
}
