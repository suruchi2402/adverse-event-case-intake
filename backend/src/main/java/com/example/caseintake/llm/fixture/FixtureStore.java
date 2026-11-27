package com.example.caseintake.llm.fixture;

import com.example.caseintake.json.JsonFormatException;
import com.example.caseintake.json.JsonIo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Recordings under fixtures/, one file per document: fixtures/&lt;DOCUMENT_ID&gt;.json.
 * Every file is scrubbed of secrets before it is written: the configured key values and anything shaped like a
 * Google API key. The key is never part of a request we record, so this is a second line of defence.
 */
public final class FixtureStore {

    public static final String REDACTED = "[REDACTED]";
    private static final Pattern GOOGLE_API_KEY = Pattern.compile("AIza[0-9A-Za-z_\\-]{35}");

    private final Path directory;
    private final List<String> secrets;

    public FixtureStore(Path directory, List<String> secrets) {
        this.directory = directory;
        this.secrets = secrets.stream().filter(s -> s != null && s.length() >= 8).toList();
    }

    public Path pathFor(String documentId) {
        return directory.resolve(documentId + ".json");
    }

    public Optional<Fixture> read(String documentId) {
        Path path = pathFor(documentId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Fixture.fromJson(JsonIo.parseObject(Files.readString(path))));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read fixture " + path, e);
        } catch (JsonFormatException e) {
            throw new FixtureException("Fixture " + path + " is not valid JSON: " + e.getMessage());
        }
    }

    public void write(Fixture fixture) {
        Path path = pathFor(fixture.documentId());
        try {
            Files.createDirectories(directory);
            Files.writeString(path, scrub(JsonIo.pretty(fixture.toJson())));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write fixture " + path, e);
        }
    }

    String scrub(String text) {
        String result = text;
        for (String secret : secrets) {
            result = result.replace(secret, REDACTED);
        }
        return GOOGLE_API_KEY.matcher(result).replaceAll(REDACTED);
    }

    public static boolean containsGoogleApiKey(String text) {
        return GOOGLE_API_KEY.matcher(text).find();
    }
}
