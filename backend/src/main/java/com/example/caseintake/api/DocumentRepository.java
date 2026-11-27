package com.example.caseintake.api;

import com.example.caseintake.config.PathsProperties;
import com.example.caseintake.extraction.SourceDocument;
import com.example.caseintake.json.JsonIo;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serves the committed files: documents, the catalog, and out/. Read from disk on each request, so a re-run of the
 * pipeline or a catalog change shows up without a restart. No database.
 */
@Component
public class DocumentRepository {

    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final Pattern SUBJECT_LINE = Pattern.compile("(?im)^\\s*(?:subject|re)\\s*:\\s*(.+?)\\s*$");
    private static final String KEY_CASES = "cases";
    private static final String KEY_FLAGS = "flags";

    private final PathsProperties paths;

    public DocumentRepository(PathsProperties paths) {
        this.paths = paths;
    }

    public record DocumentSummary(String id, String title, int caseCount, List<String> flags, boolean hasOutput, boolean hasText) {
    }

    public record DocumentDetail(String id, String title, String text, Map<String, Object> extraction) {
    }

    public Map<String, Object> catalog() {
        return JsonIo.readObject(paths.catalog());
    }

    public List<DocumentSummary> list() {
        return SourceDocument.scan(existingFolders()).stream().map(this::summarise).toList();
    }

    public DocumentDetail get(String id) {
        SourceDocument document = find(id);
        String text = document.text().map(DocumentRepository::read).orElse(null);
        return new DocumentDetail(document.id(), title(document.id(), text), text, output(document.id()).orElse(null));
    }

    private DocumentSummary summarise(SourceDocument document) {
        String text = document.text().map(DocumentRepository::read).orElse(null);
        Optional<Map<String, Object>> output = output(document.id());
        int caseCount = output.map(o -> o.get(KEY_CASES) instanceof List<?> l ? l.size() : 0).orElse(0);
        List<String> flags = output.map(o -> o.get(KEY_FLAGS) instanceof List<?> l
                ? l.stream().map(String::valueOf).toList() : List.<String>of()).orElse(List.of());
        return new DocumentSummary(document.id(), title(document.id(), text), caseCount, flags, output.isPresent(), text != null);
    }

    private SourceDocument find(String id) {
        if (!SAFE_ID.matcher(id).matches()) {
            throw new DocumentNotFoundException(id);
        }
        return SourceDocument.scan(existingFolders()).stream()
                .filter(d -> d.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    private Optional<Map<String, Object>> output(String id) {
        Path file = paths.out().resolve(id + ".json");
        return Files.exists(file) ? Optional.of(JsonIo.readObject(file)) : Optional.empty();
    }

    private List<Path> existingFolders() {
        return paths.documents().stream().filter(Files::isDirectory).toList();
    }

    /** The subject line or "Re:" line when there is one, otherwise the first non-blank line, otherwise the id. */
    static String title(String id, String text) {
        if (text == null) {
            return id;
        }
        Matcher subject = SUBJECT_LINE.matcher(text);
        if (subject.find()) {
            return subject.group(1);
        }
        return text.lines().map(String::strip).filter(l -> !l.isEmpty()).findFirst().orElse(id);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        }
    }
}
