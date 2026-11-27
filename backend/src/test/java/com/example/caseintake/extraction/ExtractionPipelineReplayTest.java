package com.example.caseintake.extraction;

import com.example.caseintake.TestData;
import com.example.caseintake.catalog.Catalog;
import com.example.caseintake.catalog.CatalogLoader;
import com.example.caseintake.json.JsonIo;
import com.example.caseintake.llm.fixture.FixtureStore;
import com.example.caseintake.llm.fixture.ReplayLlmClient;
import com.example.caseintake.result.CaseResult;
import com.example.caseintake.result.DocumentFlag;
import com.example.caseintake.result.DocumentResult;
import com.example.caseintake.result.FieldResult;
import com.example.caseintake.result.FieldStatus;
import com.example.caseintake.result.Provenance;
import com.example.caseintake.text.SourceText;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole pipeline from a recording, without network. The D01 recording is written by hand for this test and
 * contains the kinds of noise real models produce: a label instead of a code, a number as a string, a duplicate
 * list item, a quote in other case or spacing, a quote with an ellipsis, a trailing full stop.
 */
class ExtractionPipelineReplayTest {

    private static final Map<String, Object> EXPECTED_D01 = Map.ofEntries(
            Map.entry("patient_initials", "MK"), Map.entry("patient_age_years", 54L), Map.entry("patient_sex", "female"),
            Map.entry("suspect_drug_name", "amoxicillin"), Map.entry("suspect_drug_dose_value", 500L),
            Map.entry("suspect_drug_dose_unit", "mg"), Map.entry("suspect_drug_route", "oral"),
            Map.entry("suspect_drug_start_date", "2026-03-02"), Map.entry("event_terms", List.of("rash", "angioedema")),
            Map.entry("event_onset_date", "2026-03-04"), Map.entry("event_outcome", "recovered"),
            Map.entry("seriousness_criteria", List.of("hospitalization")), Map.entry("reporter_type", "physician"),
            Map.entry("report_date", "2026-03-10"), Map.entry("country", "GB"), Map.entry("concomitant_drugs", List.of()));

    private DocumentResult replayD01(Catalog catalog) {
        ExtractionPipeline pipeline = new ExtractionPipeline(catalog, new ReplayLlmClient(new FixtureStore(TestData.fixtures(), List.of())));
        return pipeline.process(SourceDocument.scan(List.of(TestData.documents())).get(0));
    }

    @Test
    void replaysD01ToTheCorrectValuesWithExactProvenance() {
        DocumentResult result = replayD01(TestData.catalog());
        assertEquals(1, result.cases().size());
        assertEquals(List.of(), result.flags());
        CaseResult c = result.cases().get(0);
        SourceText source = new SourceText(TestData.read(TestData.documents().resolve("D01.txt")));
        EXPECTED_D01.forEach((id, expected) -> {
            FieldResult field = c.fields().get(id);
            assertEquals(FieldStatus.EXTRACTED, field.status(), id + ": " + field.note());
            assertEquals(expected, field.value(), id);
            for (Provenance p : field.provenance()) {
                assertEquals(p.quote(), source.slice(p.start(), p.end()), id);
            }
        });
        assertEquals(2, c.fields().get("event_terms").provenance().size(), "ellipsis quote gives two spans");
    }

    @Test
    void everyCatalogFieldAppearsInCatalogOrder() {
        Catalog catalog = TestData.catalog();
        CaseResult c = replayD01(catalog).cases().get(0);
        assertEquals(catalog.fields().stream().map(f -> f.id()).toList(), List.copyOf(c.fields().keySet()));
    }

    @Test
    void aFieldAddedToTheCatalogComesOutUnknownWithAReason() {
        Catalog changed = CatalogLoader.parse(TestData.catalogJson().replace("\"fields\": [",
                "\"fields\": [{\"id\": \"patient_weight_kg\", \"label\": \"Weight (kg)\", \"section\": \"patient\", \"type\": \"number\"},"));
        FieldResult weight = replayD01(changed).cases().get(0).fields().get("patient_weight_kg");
        assertEquals(FieldStatus.UNKNOWN, weight.status());
        assertEquals(FieldResolver.NOT_COVERED, weight.note());
    }

    @Test
    void aRecordedValueThatNoLongerPassesTheCatalogComesOutUnknown() {
        Catalog stricter = CatalogLoader.parse(TestData.catalogJson().replace("\"max\": 120", "\"max\": 50"));
        FieldResult age = replayD01(stricter).cases().get(0).fields().get("patient_age_years");
        assertEquals(FieldStatus.UNKNOWN, age.status());
        assertTrue(age.note().contains("above max"), age.note());
    }

    @Test
    void aDocumentWithoutTextIsUnsupportedAndHasNoCases() throws IOException {
        Path docs = TestData.tempDir();
        Files.writeString(docs.resolve("S03.pdf"), "%PDF-1.4 image only");
        ExtractionPipeline pipeline = new ExtractionPipeline(TestData.catalog(), request -> {
            throw new AssertionError("the model must not be called");
        });
        DocumentResult result = pipeline.process(SourceDocument.scan(List.of(docs)).get(0));
        assertEquals(List.of(DocumentFlag.UNSUPPORTED_FORMAT.jsonName()), result.flags());
        assertTrue(result.cases().isEmpty());
    }

    @Test
    void aChangedDocumentIsNotReplayedFromAnOldRecording() throws IOException {
        Path docs = TestData.copyOf(TestData.documents());
        Files.writeString(docs.resolve("D01.txt"), TestData.read(docs.resolve("D01.txt")) + "\nAddendum.");
        Path out = TestData.tempDir();
        ExtractionPipeline pipeline = new ExtractionPipeline(TestData.catalog(), new ReplayLlmClient(new FixtureStore(TestData.fixtures(), List.of())));
        ExtractionPipeline.RunReport report = pipeline.run(List.of(docs), out);
        assertFalse(report.ok());
        assertTrue(report.failures().get(0).contains("different version"), report.failures().toString());
        assertFalse(Files.exists(out.resolve("D01.json")));
    }

    @Test
    void replayIsDeterministic() {
        Path outA = TestData.tempDir();
        Path outB = TestData.tempDir();
        for (Path out : List.of(outA, outB)) {
            new ExtractionPipeline(TestData.catalog(), new ReplayLlmClient(new FixtureStore(TestData.fixtures(), List.of())))
                    .run(List.of(TestData.documents()), out);
        }
        assertEquals(TestData.read(outA.resolve("D01.json")), TestData.read(outB.resolve("D01.json")));
        assertEquals("D01", JsonIo.readObject(outA.resolve("D01.json")).get("document_id"));
    }
}
