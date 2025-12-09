package edu.sdle.crdt.collections;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import edu.sdle.crdt.ICRDT;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class AWMapDeserializer<K, V extends ICRDT<V>> extends JsonDeserializer<AWMap<?, ?>>
        implements ContextualDeserializer {
    private JavaType keyType;
    private JavaType valueType;

    public AWMapDeserializer() {}

    private AWMapDeserializer(JavaType keyType, JavaType valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                                                BeanProperty property) {
        JavaType type = ctxt.getContextualType();
        if (type == null && property != null) {
            type = property.getType();
        }

        if (type != null && type.containedTypeCount() >= 2) {
            JavaType keyType = type.containedType(0);
            JavaType valueType = type.containedType(1);
            return new AWMapDeserializer<>(keyType, valueType);
        }

        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AWMap<K, V> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode root = codec.readTree(p);
        ObjectMapper mapper = (ObjectMapper) p.getCodec();

        AWMap<K, V> map = new AWMap<>();

        // Parse items
        JsonNode items = root.get("map");
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                // Parse key
                K key;
                if (keyType != null) {
                    key = mapper.convertValue(item.get("key"), keyType);
                } else {
                    key = mapper.convertValue(item.get("key"), new TypeReference<>() {});
                }

                // Parse value
                V value;
                if (valueType != null) {
                    value = mapper.convertValue(item.get("value"), valueType);
                } else {
                    value = (V) mapper.convertValue(item.get("value"), ICRDT.class);
                }

                Map<UUID, Integer> dots = mapper.convertValue(item.get("dots"),
                        new TypeReference<>() {});
                var entry = new AWMapEntry<>(key, value);
                map.awSet.items.put(entry, dots);
            }
        }

        // Parse version dots
        JsonNode version = root.get("version");
        if (version != null && version.isObject()) {
            Map<UUID, Integer> versionDots =
                    ((ObjectMapper) codec).convertValue(version, new TypeReference<>() {});
            map.awSet.versionDots.putAll(versionDots);
        }

        return map;
    }
}