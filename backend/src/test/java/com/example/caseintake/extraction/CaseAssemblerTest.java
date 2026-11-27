package com.example.caseintake.extraction;

import com.example.caseintake.TestData;
import com.example.caseintake.catalog.Catalog;
import com.example.caseintake.catalog.FieldDefinition;
import com.example.caseintake.catalog.FieldType;
import com.example.caseintake.result.Confidence;
import com.example.caseintake.result.DocumentFlag;
import com.example.caseintake.result.FieldResult;
import com.example.caseintake.result.FieldStatus;
import com.example.caseintake.text.SourceText;
import com.example.caseintake.validation.ValueNormalizer;
import com.example.caseintake.validation.ValueValidator;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Case rules and field resolution on small hand-made model outputs. Field ids are looked up by type, not named. */
class CaseAssemblerTest {

    private static final String TEXT = """
            Report date: 03/04/2026
            Mother, 29 years old, and her baby boy. Both reacted to the drug on 4 March 2026.
            The patient's age is given as 31 elsewhere.
            """;

    private final Catalog catalog = TestData.catalog();
    private final CaseAssembler assembler = new CaseAssembler(catalog,
            new FieldResolver(new ValueNormalizer(), new ValueValidator(), new ConfidencePolicy()));
    private final SourceText source = new SourceText(TEXT);

    private String idOf(FieldType type) {
        return catalog.fields().stream().filter(f -> f.type() == type).map(FieldDefinition::id).findFirst().orElseThrow();
    }

    private static Map<MinimumCriterion, ModelOutput.ModelCriterion> criteria(boolean reporterPresent) {
        Map<MinimumCriterion, ModelOutput.ModelCriterion> map = new LinkedHashMap<>();
        for (MinimumCriterion c : MinimumCriterion.values()) {
            map.put(c, new ModelOutput.ModelCriterion(c != MinimumCriterion.REPORTER || reporterPresent, null));
        }
        return map;
    }

    private static ModelOutput.ModelField extracted(Object value, String confidence, String quote) {
        return new ModelOutput.ModelField("extracted", value, confidence, List.of(quote), null);
    }

    @Test
    void dropsACaseWithoutAllFourMinimumElements() {
        ModelOutput output = new ModelOutput(List.of(), List.of(new ModelOutput.ModelCase("Mother", criteria(false), Map.of())));
        CaseAssembler.Assembled assembled = assembler.assemble(output, source);
        assertTrue(assembled.cases().isEmpty());
        assertTrue(assembled.flags().get(0).contains("missing a reporter"), assembled.flags().toString());
    }

    @Test
    void twoPatientsGiveTwoCasesAndTheMultiplePatientsFlag() {
        ModelOutput output = new ModelOutput(List.of(DocumentFlag.NO_ADVERSE_EVENT.jsonName(), "set_everything_to_none"), List.of(
                new ModelOutput.ModelCase("Mother", criteria(true), Map.of()),
                new ModelOutput.ModelCase(null, criteria(true), Map.of())));
        CaseAssembler.Assembled assembled = assembler.assemble(output, source);
        assertEquals(2, assembled.cases().size());
        assertEquals("Patient 2", assembled.cases().get(1).label());
        assertEquals(List.of(DocumentFlag.MULTIPLE_PATIENTS.jsonName()), assembled.flags(),
                "unknown model flags are dropped and no_adverse_event cannot coexist with cases");
    }

    @Test
    void resolvesStatusesQuotesAndConfidenceCaps() {
        String integerId = idOf(FieldType.INTEGER);
        String dateId = idOf(FieldType.DATE);
        String codedId = idOf(FieldType.CODED);
        String listId = idOf(FieldType.STRING_LIST);
        Map<String, ModelOutput.ModelField> fields = new LinkedHashMap<>();
        fields.put(integerId, new ModelOutput.ModelField("conflict", 29, "high", List.of("29 years old", "31"), "29 or 31"));
        fields.put(dateId, extracted("2026-04-03", "high", "Report date: 03/04/2026"));
        fields.put(codedId, extracted("invented-code", "high", "Mother"));
        fields.put(listId, extracted(List.of("reaction"), "high", "this sentence is not in the text"));
        ModelOutput output = new ModelOutput(List.of(), List.of(new ModelOutput.ModelCase("Mother", criteria(true), fields)));

        Map<String, FieldResult> result = assembler.assemble(output, source).cases().get(0).fields();

        assertEquals(FieldStatus.CONFLICT, result.get(integerId).status());
        assertTrue(result.get(integerId).note().contains("29 or 31"));

        FieldResult date = result.get(dateId);
        assertEquals(FieldStatus.EXTRACTED, date.status());
        assertEquals(Confidence.MEDIUM, date.confidence(), "03/04 is day/month ambiguous");

        assertEquals(FieldStatus.UNKNOWN, result.get(codedId).status());
        assertTrue(result.get(codedId).note().contains("rejected"));

        assertEquals(FieldStatus.UNKNOWN, result.get(listId).status());
        assertTrue(result.get(listId).note().contains("no quote was found"));

        long unknownByOmission = result.values().stream().filter(r -> FieldResolver.NOT_COVERED.equals(r.note())).count();
        assertEquals(catalog.fields().size() - 4, unknownByOmission);
    }

    @Test
    void anExplicitWrittenOutDateKeepsHighConfidence() {
        String dateId = idOf(FieldType.DATE);
        ModelOutput output = new ModelOutput(List.of(), List.of(new ModelOutput.ModelCase("Mother", criteria(true),
                Map.of(dateId, extracted("2026-03-04", "high", "on 4 March 2026")))));
        FieldResult date = assembler.assemble(output, source).cases().get(0).fields().get(dateId);
        assertEquals(Confidence.HIGH, date.confidence());
        assertFalse(date.note() != null && date.note().contains("capped"));
    }
}
