package edu.sdle.crdt;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;

public class AWMapSerializer<K, V extends ICRDT<V>> extends JsonSerializer<AWMap<K, V>> {
    @Override
    public void serialize(AWMap<K, V> map, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeFieldName("map");
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