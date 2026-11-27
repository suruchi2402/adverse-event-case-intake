package com.example.caseintake;

import com.example.caseintake.llm.fixture.FixtureStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fails the build if anything that looks like a Google API key is in the committed fixtures or outputs. */
class RepositorySecretsTest {

    @Test
    void noApiKeyInFixturesOrOutputs() throws IOException {
        Path repositoryRoot = Path.of("").toAbsolutePath().getParent(); // tests run in backend/
        for (String folder : List.of("fixtures", "out")) {
            Path dir = repositoryRoot.resolve(folder);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    assertTrue(!FixtureStore.containsGoogleApiKey(Files.readString(file)), "API key found in " + file);
                }
            }
        }
    }
}
