package com.example.caseintake.extraction;

import com.example.caseintake.catalog.FieldDefinition;
import com.example.caseintake.catalog.FieldDefinition.FieldValidation;
import com.example.caseintake.catalog.FieldType;
import com.example.caseintake.result.Confidence;
import com.example.caseintake.text.DateMentions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfidencePolicyTest {

    private final ConfidencePolicy policy = new ConfidencePolicy();
    private static final FieldDefinition DATE = new FieldDefinition("d", "D", "s", FieldType.DATE, List.of(), FieldValidation.NONE, null);
    private static final FieldDefinition INT = new FieldDefinition("i", "I", "s", FieldType.INTEGER, List.of(), FieldValidation.NONE, null);

    @Test
    void readsCommonDateForms() {
        assertEquals(LocalDate.of(2026, 3, 30), DateMentions.find("started it on March 30th, 2026").get(0).date());
        assertEquals(LocalDate.of(2026, 3, 4), DateMentions.find("On 4 March 2026 she").get(0).date());
        assertEquals(LocalDate.of(2026, 1, 12), DateMentions.find("12.01.2026   DIGOXIN").get(0).date());
        assertTrue(DateMentions.find("12.01.2026").get(0).ambiguous());
        assertEquals(1, DateMentions.find("29/04/2026").size(), "29 cannot be a month");
    }

    @Test
    void writtenOutDatesAreNotCapped() {
        assertTrue(policy.capFor(DATE, "2026-03-04", List.of("On 4 March 2026 she developed")).isEmpty());
        assertTrue(policy.capFor(DATE, "2026-04-29", List.of("on 29/04/2026")).isEmpty());
    }

    @Test
    void computedOrAmbiguousDatesAreCappedAtMedium() {
        assertEquals(Confidence.MEDIUM, policy.capFor(DATE, "2026-04-09", List.of("About a week and a half in")).orElseThrow().max());
        assertEquals(Confidence.MEDIUM, policy.capFor(DATE, "2026-04-03", List.of("Treatment started: 03/04/2026")).orElseThrow().max());
    }

    @Test
    void numbersMustAppearInTheQuote() {
        assertTrue(policy.capFor(INT, 54L, List.of("a 54-year-old woman")).isEmpty());
        assertEquals(Confidence.MEDIUM, policy.capFor(INT, 72L, List.of("Date of birth: 1953")).orElseThrow().max());
        assertEquals(Confidence.MEDIUM, policy.capFor(INT, 5L, List.of("500 mg")).orElseThrow().max(), "5 is not 500");
    }
}
