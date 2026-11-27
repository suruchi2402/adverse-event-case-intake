package com.example.caseintake.validation;

import com.example.caseintake.catalog.FieldDefinition;
import com.example.caseintake.catalog.FieldDefinition.FieldOption;
import com.example.caseintake.catalog.FieldDefinition.FieldValidation;
import com.example.caseintake.catalog.FieldType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueNormalizerTest {

    private final ValueNormalizer normalizer = new ValueNormalizer();

    private static FieldDefinition field(FieldType type, List<FieldOption> options) {
        return new FieldDefinition("f", "F", "s", type, options, FieldValidation.NONE, null);
    }

    private static final List<FieldOption> OPTIONS = List.of(new FieldOption("not_recovered", "Not recovered"), new FieldOption("fatal", "Fatal"));

    @Test
    void numbersFromStringsAndIntegralDoubles() {
        assertEquals(54L, normalizer.normalize(field(FieldType.INTEGER, List.of()), "54"));
        assertEquals(54L, normalizer.normalize(field(FieldType.INTEGER, List.of()), 54.0));
        assertEquals(500L, normalizer.normalize(field(FieldType.NUMBER, List.of()), 500.0));
        assertEquals(0.25, normalizer.normalize(field(FieldType.NUMBER, List.of()), "0.250"));
        assertThrows(InvalidValueException.class, () -> normalizer.normalize(field(FieldType.INTEGER, List.of()), 54.5));
        assertThrows(InvalidValueException.class, () -> normalizer.normalize(field(FieldType.NUMBER, List.of()), "about 5"));
    }

    @Test
    void codesFromLabelsOrOtherCase() {
        FieldDefinition coded = field(FieldType.CODED, OPTIONS);
        assertEquals("not_recovered", normalizer.normalize(coded, "Not recovered"));
        assertEquals("fatal", normalizer.normalize(coded, "FATAL"));
        assertThrows(InvalidValueException.class, () -> normalizer.normalize(coded, "died"));
    }

    @Test
    void multiCodesAreDedupedAndInCatalogOrder() {
        FieldDefinition multi = field(FieldType.CODED_MULTI, OPTIONS);
        assertEquals(List.of("not_recovered", "fatal"), normalizer.normalize(multi, List.of("fatal", "Not recovered", "fatal")));
        assertEquals(List.of("fatal"), normalizer.normalize(multi, "fatal"));
    }

    @Test
    void stringListsAreTrimmedAndDeduped() {
        assertEquals(List.of("rash", "angioedema"),
                normalizer.normalize(field(FieldType.STRING_LIST, List.of()), List.of(" rash", "angioedema", "Rash", "")));
    }

    @Test
    void blankAndMissingValuesAreRejected() {
        assertThrows(InvalidValueException.class, () -> normalizer.normalize(field(FieldType.STRING, List.of()), "  "));
        assertThrows(InvalidValueException.class, () -> normalizer.normalize(field(FieldType.STRING, List.of()), null));
    }
}
