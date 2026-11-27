package com.example.caseintake;

import com.example.caseintake.catalog.Catalog;
import com.example.caseintake.catalog.CatalogLoader;
import com.example.caseintake.config.LlmProperties;
import com.example.caseintake.config.PathsProperties;
import com.example.caseintake.extraction.ExtractionPipeline;
import com.example.caseintake.extraction.ExtractionPipeline.RunReport;
import com.example.caseintake.llm.LlmClient;
import com.example.caseintake.llm.LlmClientFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * The extraction command: {@code java -jar case-intake.jar extract [options]}.
 * <pre>
 *   --provider=gemini|replay   overrides llm.provider
 *   --docs=dir1,dir2           documents folders (default: case-intake.documents)
 *   --catalog=path             catalog file (default: case-intake.catalog)
 *   --out=dir                  output folder (default: case-intake.out)
 *   --fixtures=dir             recordings folder (default: case-intake.fixtures)
 *   --refresh                  call the model even when a recording exists for the document
 * </pre>
 */
@Component
@Profile(CaseIntakeApplication.EXTRACT_PROFILE)
public class ExtractCommand implements ApplicationRunner {

    private static final Logger LOG = Logger.getLogger(ExtractCommand.class.getName());

    private final LlmProperties llmProperties;
    private final PathsProperties paths;

    public ExtractCommand(LlmProperties llmProperties, PathsProperties paths) {
        this.llmProperties = llmProperties;
        this.paths = paths;
    }

    @Override
    public void run(ApplicationArguments args) {
        String provider = option(args, "provider", llmProperties.provider());
        List<Path> docs = args.containsOption("docs")
                ? Arrays.stream(option(args, "docs", "").split(",")).map(String::strip).filter(s -> !s.isEmpty()).map(Path::of).toList()
                : paths.documents();
        Path catalogPath = Path.of(option(args, "catalog", paths.catalog().toString()));
        Path out = Path.of(option(args, "out", paths.out().toString()));
        Path fixtures = Path.of(option(args, "fixtures", paths.fixtures().toString()));
        boolean refresh = args.containsOption("refresh");

        Catalog catalog = CatalogLoader.load(catalogPath);
        LlmProperties effective = new LlmProperties(provider, llmProperties.gemini(), llmProperties.rateLimit(), llmProperties.retry());
        LlmClient client = LlmClientFactory.create(effective, fixtures, refresh, System::getenv);

        LOG.info(() -> "Extracting " + docs + " with provider " + provider + ", catalog " + catalogPath + " (version " + catalog.version() + ")");
        RunReport report = new ExtractionPipeline(catalog, client).run(docs, out);
        LOG.info(() -> "Wrote " + report.written().size() + " output file(s) to " + out);
        if (!report.ok()) {
            throw new ExtractionFailedException(report.failures());
        }
    }

    private static String option(ApplicationArguments args, String name, String fallback) {
        List<String> values = args.getOptionValues(name);
        return values == null || values.isEmpty() ? fallback : values.get(values.size() - 1);
    }

    /** At least one document could not be processed; the message lists them. */
    public static class ExtractionFailedException extends RuntimeException {
        public ExtractionFailedException(List<String> failures) {
            super("Extraction failed for " + failures.size() + " item(s):\n  " + String.join("\n  ", failures));
        }
    }
}
