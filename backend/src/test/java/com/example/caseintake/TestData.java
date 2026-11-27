package com.example.caseintake;

import com.example.caseintake.catalog.Catalog;
import com.example.caseintake.catalog.CatalogLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Test files under src/test/resources/testdata: a copy of the catalog, D01.txt, and a hand-written D01 recording. */
public final class TestData {

    private TestData() {
    }

    public static Path root() {
        try {
            return Path.of(TestData.class.getResource("/testdata").toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Path catalogPath() {
        return root().resolve("catalog.json");
    }

    public static Catalog catalog() {
        return CatalogLoader.load(catalogPath());
    }

    public static String catalogJson() {
        return read(catalogPath());
    }

    public static Path documents() {
        return root().resolve("documents");
    }

    public static Path fixtures() {
        return root().resolve("fixtures");
    }

    public static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A fresh temporary folder holding a copy of the given folder. */
    public static Path copyOf(Path folder) {
        try {
            Path target = Files.createTempDirectory("case-intake-test");
            try (Stream<Path> files = Files.walk(folder)) {
                for (Path p : files.sorted(Comparator.naturalOrder()).toList()) {
                    Path dest = target.resolve(folder.relativize(p).toString());
                    if (Files.isDirectory(p)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.copy(p, dest);
                    }
                }
            }
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Path tempDir() {
        try {
            return Files.createTempDirectory("case-intake-test");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
