package com.example.caseintake.extraction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * A document in a documents folder, identified by its file name without extension. The pipeline reads the .txt;
 * a document that exists only in another format (for example an image-only PDF) has no text.
 */
public record SourceDocument(String id, Path textFile, Path otherFile) {

    public static final String TEXT_EXTENSION = ".txt";

    public Optional<Path> text() {
        return Optional.ofNullable(textFile);
    }

    /** Documents directly inside each folder, sorted by id. Subfolders are separate folders and are not searched. */
    public static List<SourceDocument> scan(List<Path> folders) {
        Map<String, Path[]> byId = new TreeMap<>();
        for (Path folder : folders) {
            if (!Files.isDirectory(folder)) {
                throw new IllegalArgumentException("Not a documents folder: " + folder);
            }
            try (Stream<Path> files = Files.list(folder)) {
                files.filter(Files::isRegularFile).forEach(file -> {
                    String name = file.getFileName().toString();
                    int dot = name.lastIndexOf('.');
                    if (dot <= 0) {
                        return;
                    }
                    String id = name.substring(0, dot);
                    Path[] slot = byId.computeIfAbsent(id, k -> new Path[2]);
                    if (name.toLowerCase(Locale.ROOT).endsWith(TEXT_EXTENSION)) {
                        slot[0] = file;
                    } else if (slot[1] == null) {
                        slot[1] = file;
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot list " + folder, e);
            }
        }
        List<SourceDocument> documents = new ArrayList<>();
        byId.forEach((id, slot) -> documents.add(new SourceDocument(id, slot[0], slot[1])));
        return documents;
    }
}
