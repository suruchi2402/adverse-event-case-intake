package com.example.caseintake.extraction;

import com.example.caseintake.catalog.Catalog;
import com.example.caseintake.json.JsonIo;
import com.example.caseintake.llm.LlmClient;
import com.example.caseintake.llm.LlmException;
import com.example.caseintake.llm.LlmRequest;
import com.example.caseintake.llm.LlmResponse;
import com.example.caseintake.result.DocumentFlag;
import com.example.caseintake.result.DocumentResult;
import com.example.caseintake.text.SourceText;
import com.example.caseintake.validation.ValueNormalizer;
import com.example.caseintake.validation.ValueValidator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.Logger;

/**
 * Document in, output file out: one model call per document (the longest is about 12,700 characters, well inside
 * any current context window, so there is no chunking), then deterministic checks. The same code runs live and in
 * replay; only the {@link LlmClient} differs. A document that fails is reported and the run goes on; the run then
 * ends with an error so that a partial result is never mistaken for a complete one.
 */
public final class ExtractionPipeline {

    private static final Logger LOG = Logger.getLogger(ExtractionPipeline.class.getName());

    private final Catalog catalog;
    private final LlmClient llm;
    private final PromptBuilder prompts;
    private final CaseAssembler assembler;

    public ExtractionPipeline(Catalog catalog, LlmClient llm) {
        this.catalog = catalog;
        this.llm = llm;
        this.prompts = new PromptBuilder(catalog);
        this.assembler = new CaseAssembler(catalog,
                new FieldResolver(new ValueNormalizer(), new ValueValidator(), new ConfidencePolicy()));
    }

    public record RunReport(List<String> written, List<String> failures) {
        public boolean ok() {
            return failures.isEmpty();
        }
    }

    public RunReport run(List<Path> documentFolders, Path outDir) {
        List<String> written = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (SourceDocument document : SourceDocument.scan(documentFolders)) {
            try {
                DocumentResult result = process(document);
                JsonIo.write(outDir.resolve(document.id() + ".json"), result.toJson());
                written.add(document.id());
                LOG.info(() -> document.id() + ": " + result.cases().size() + " case(s), flags " + result.flags());
            } catch (LlmException | ModelOutputException e) {
                failures.add(document.id() + ": " + e.getMessage());
                LOG.severe(() -> document.id() + " failed: " + e.getMessage());
                if (e instanceof LlmException.QuotaExhausted) {
                    failures.add("Stopped: daily quota exhausted. Run again later; recorded documents are reused.");
                    break;
                }
            }
        }
        return new RunReport(written, failures);
    }

    public DocumentResult process(SourceDocument document) {
        if (document.text().isEmpty()) {
            return new DocumentResult(document.id(), catalog.version(), null,
                    List.of(DocumentFlag.UNSUPPORTED_FORMAT.jsonName()), List.of());
        }
        String text = readUtf8(document.text().get());
        SourceText source = new SourceText(text);
        LlmRequest request = prompts.build(document.id(), source, sha256(text));
        LlmResponse response = llm.complete(request);
        CaseAssembler.Assembled assembled = assembler.assemble(ModelOutput.parse(response.text()), source);
        return new DocumentResult(document.id(), catalog.version(), response.model(), assembled.flags(), assembled.cases());
    }

    private static String readUtf8(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        }
    }

    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
