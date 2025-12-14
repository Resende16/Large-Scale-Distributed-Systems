package edu.sdle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class JsonUtils {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {}

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize " + obj.getClass().getSimpleName() + " to json!\n" + e.getMessage());
            return null;
        }
    }

    public static boolean toJson(Object obj, File file) {
        try {
            MAPPER.writeValue(file, obj);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to serialize " + obj.getClass().getSimpleName() + " to json file!\n" + e.getMessage());
            return false;
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to deserialize json to " + clazz.getSimpleName() + "!\n" +  e.getMessage());
            return null;
        }
    }

    public static <T> T fromJson(File file, Class<T> clazz) {
        try {
            return MAPPER.readValue(file, clazz);
        } catch (IOException e) {
            System.err.println("Failed to deserialize json file to " + clazz.getSimpleName() + "!\n" + e.getMessage());
            return null;
        }
    }
}
