package com.example.caseintake.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * JSON as plain Java values: Map (insertion-ordered), List, String, Number, Boolean, null.
 * The pipeline works on these so that every step is easy to test and the output order is deterministic.
 */
public final class JsonIo {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectWriter PRETTY = MAPPER.writerWithDefaultPrettyPrinter();

    private JsonIo() {
    }

    public static Object parse(String json) {
        try {
            return MAPPER.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new JsonFormatException("Not valid JSON: " + e.getOriginalMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object value = parse(json);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new JsonFormatException("Expected a JSON object", null);
    }

    public static Map<String, Object> readObject(Path path) {
        try {
            return parseObject(Files.readString(path));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        }
    }

    public static String pretty(Object value) {
        try {
            return PRETTY.writeValueAsString(value) + "\n";
        } catch (JsonProcessingException e) {
            throw new JsonFormatException("Cannot serialise value", e);
        }
    }

    public static String compact(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new JsonFormatException("Cannot serialise value", e);
        }
    }

    public static void write(Path path, Object value) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(path, pretty(value));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + path, e);
        }
    }
}
