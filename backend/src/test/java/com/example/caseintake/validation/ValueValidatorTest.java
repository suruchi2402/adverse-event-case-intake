package com.example.caseintake.validation;

import com.example.caseintake.TestData;
import com.example.caseintake.catalog.Catalog;
import com.example.caseintake.catalog.FieldDefinition;
import com.example.caseintake.catalog.FieldType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks are by field type, using whichever catalog field has that type, so no field id is named here either. */
class ValueValidatorTest {

    private final Catalog catalog = TestData.catalog();
    private final ValueValidator validator = new ValueValidator();

    private FieldDefinition firstOf(FieldType type, boolean withPattern) {
        return catalog.fields().stream()
                .filter(f -> f.type() == type && f.validation().patternRule().isPresent() == withPattern)
                .findFirst().orElseThrow();
    }

    @Test
    void stringPattern() {
        FieldDefinition patterned = firstOf(FieldType.STRING, true);
        assertTrue(validator.validate(patterned, "AB").isEmpty());
        assertFalse(validator.validate(patterned, "m.k.").isEmpty());
        assertFalse(validator.validate(patterned, 12).isEmpty());
    }

    @Test
    void integerRangeAndIntegrality() {
        FieldDefinition integer = catalog.fields().stream()
                .filter(f -> f.type() == FieldType.INTEGER && f.validation().maxRule().isPresent()).findFirst().orElseThrow();
        assertTrue(validator.validate(integer, 54L).isEmpty());
        assertFalse(validator.validate(integer, 54.5).isEmpty());
        assertFalse(validator.validate(integer, (long) (integer.validation().max() + 1)).isEmpty());
        assertFalse(validator.validate(integer, true).isEmpty());
    }

    @Test
    void numberMinimum() {
        FieldDefinition number = firstOf(FieldType.NUMBER, false);
        assertTrue(validator.validate(number, 0.25).isEmpty());
        assertFalse(validator.validate(number, -1L).isEmpty());
    }

    @Test
    void datesMustBeIsoAndReal() {
        FieldDefinition date = firstOf(FieldType.DATE, false);
        assertTrue(validator.validate(date, "2026-03-04").isEmpty());
        assertFalse(validator.validate(date, "04/03/2026").isEmpty());
        assertFalse(validator.validate(date, "2026-02-30").isEmpty());
    }

    @Test
    void codedValuesMustBeOptionCodes() {
        FieldDefinition coded = firstOf(FieldType.CODED, false);
        assertTrue(validator.validate(coded, coded.optionCodes().get(0)).isEmpty());
        assertFalse(validator.validate(coded, "not-a-code").isEmpty());
    }

    @Test
    void exclusiveOptionStandsAlone() {
        FieldDefinition multi = firstOf(FieldType.CODED_MULTI, false);
        String other = multi.optionCodes().stream().filter(c -> !c.equals(ValueValidator.EXCLUSIVE_OPTION_CODE)).findFirst().orElseThrow();
        assertTrue(validator.validate(multi, List.of(other)).isEmpty());
        assertTrue(validator.validate(multi, List.of(ValueValidator.EXCLUSIVE_OPTION_CODE)).isEmpty());
        assertFalse(validator.validate(multi, List.of(ValueValidator.EXCLUSIVE_OPTION_CODE, other)).isEmpty());
        assertFalse(validator.validate(multi, List.of()).isEmpty());
    }

    @Test
    void stringListAcceptsEmptyList() {
        FieldDefinition list = firstOf(FieldType.STRING_LIST, false);
        assertTrue(validator.validate(list, List.of()).isEmpty());
        assertFalse(validator.validate(list, List.of(1, 2)).isEmpty());
    }
}
