package com.example.caseintake.text;

import com.example.caseintake.TestData;
import com.example.caseintake.json.JsonIo;
import com.example.caseintake.text.QuoteLocator.Span;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuoteLocatorTest {

    private static final SourceText D01 = new SourceText(TestData.read(TestData.documents().resolve("D01.txt")));

    @Test
    void findsEveryQuoteOfTheExampleOutputAtItsExactPosition() {
        Map<String, Object> output = JsonIo.readObject(TestData.root().resolve("EXAMPLE-output-D01.json"));
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> fields = (Map<String, Map<String, Object>>)
                ((Map<String, Object>) ((List<?>) output.get("cases")).get(0)).get("fields");
        int checked = 0;
        for (Map<String, Object> field : fields.values()) {
            for (Object p : (List<?>) field.get("provenance")) {
                Map<?, ?> prov = (Map<?, ?>) p;
                Span span = QuoteLocator.locate(D01, (String) prov.get("quote")).orElseThrow().get(0);
                assertEquals(((Number) prov.get("start")).intValue(), span.start(), "start of " + prov.get("quote"));
                assertEquals(((Number) prov.get("end")).intValue(), span.end(), "end of " + prov.get("quote"));
                checked++;
            }
        }
        assertEquals(16, checked);
    }

    @Test
    void ignoresCaseAndRunsOfWhitespace() {
        Span span = QuoteLocator.locate(D01, "A 54-YEAR-OLD    woman").orElseThrow().get(0);
        assertEquals("a 54-year-old woman", D01.slice(span.start(), span.end()));
    }

    @Test
    void matchesAcrossLineBreaksInTheSource() {
        SourceText text = new SourceText("Dr Jane Doe\nConsultant\n   Dermatologist\n");
        Span span = QuoteLocator.locate(text, "Consultant Dermatologist").orElseThrow().get(0);
        assertEquals("Consultant\n   Dermatologist", text.slice(span.start(), span.end()));
    }

    @Test
    void splitsAQuoteShortenedWithAnEllipsis() {
        List<Span> spans = QuoteLocator.locate(D01, "widespread pruritic maculopapular rash ... swelling of the lips").orElseThrow();
        assertEquals(2, spans.size());
        assertEquals("swelling of the lips", D01.slice(spans.get(1).start(), spans.get(1).end()));
    }

    @Test
    void toleratesATrailingFullStopAndSurroundingQuotationMarks() {
        assertTrue(QuoteLocator.locate(D01, "\"She took the first dose on 2 March 2026.\"").isPresent());
        assertTrue(QuoteLocator.locate(D01, "Consultant Dermatologist.").isPresent());
    }

    @Test
    void rejectsQuotesThatAreNotInTheDocumentOrTooShort() {
        assertTrue(QuoteLocator.locate(D01, "She was admitted to intensive care").isEmpty());
        assertTrue(QuoteLocator.locate(D01, "Dr").isEmpty());
        assertTrue(QuoteLocator.locate(D01, null).isEmpty());
    }

    @Test
    void countsOffsetsInCodePointsAndFoldsTypographicDashes() {
        SourceText text = new SourceText("😀 Rash – severe");
        Span span = QuoteLocator.locate(text, "rash - severe").orElseThrow().get(0);
        assertEquals(2, span.start());
        assertEquals("Rash – severe", text.slice(span.start(), span.end()));
    }
}
