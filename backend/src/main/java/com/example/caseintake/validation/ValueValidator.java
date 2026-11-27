package com.example.caseintake.validation;

import com.example.caseintake.catalog.FieldDefinition;
import com.example.caseintake.catalog.FieldDefinition.FieldValidation;
import com.example.caseintake.catalog.FieldType;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Checks a value against its catalog field. Mirrors tools/check.py (Catalog.validate) so that anything this class
 * accepts the checker accepts too; dates are also checked against the calendar, which is stricter.
 */
public final class ValueValidator {

    /** The option code that means "no criterion applies" and may not be combined with others (see the catalog guidance). */
    public static final String EXCLUSIVE_OPTION_CODE = "none";

    private static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    /** Why the value is invalid, or empty when it is valid. */
    public Optional<String> validate(FieldDefinition field, Object value) {
        return switch (field.type()) {
            case STRING -> value instanceof String s ? checkPattern(field.validation(), s) : Optional.of("string expected");
            case INTEGER, NUMBER -> checkNumber(field, value);
            case DATE -> checkDate(value);
            case CODED -> value instanceof String s && field.optionCodes().contains(s)
                    ? Optional.empty()
                    : Optional.of("not an option code: " + value);
            case CODED_MULTI -> checkCodedMulti(field, value);
            case STRING_LIST -> checkStringList(field.validation(), value);
        };
    }

    private static Optional<String> checkPattern(FieldValidation validation, String value) {
        return validation.patternRule()
                .filter(p -> !p.matcher(value).lookingAt()) // Python re.match semantics: anchored at the start
                .map(p -> "does not match pattern " + p.pattern());
    }

    private static Optional<String> checkNumber(FieldDefinition field, Object value) {
        if (!(value instanceof Number n) || value instanceof Boolean) {
            return Optional.of("number expected");
        }
        double d = n.doubleValue();
        if (!Double.isFinite(d)) {
            return Optional.of("finite number expected");
        }
        if (field.type() == FieldType.INTEGER && d != Math.rint(d)) {
            return Optional.of("integer expected");
        }
        FieldValidation v = field.validation();
        if (v.minRule().isPresent() && d < v.min()) {
            return Optional.of("below min " + v.min());
        }
        if (v.maxRule().isPresent() && d > v.max()) {
            return Optional.of("above max " + v.max());
        }
        return Optional.empty();
    }

    private static Optional<String> checkDate(Object value) {
        if (!(value instanceof String s) || !ISO_DATE.matcher(s).matches()) {
            return Optional.of("ISO date YYYY-MM-DD expected");
        }
        try {
            LocalDate.parse(s);
            return Optional.empty();
        } catch (DateTimeParseException e) {
            return Optional.of("not a calendar date: " + s);
        }
    }

    private static Optional<String> checkCodedMulti(FieldDefinition field, Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return Optional.of("non-empty list of option codes expected");
        }
        List<?> bad = list.stream().filter(x -> !(x instanceof String s && field.optionCodes().contains(s))).toList();
        if (!bad.isEmpty()) {
            return Optional.of("not option codes: " + bad);
        }
        if (list.contains(EXCLUSIVE_OPTION_CODE) && list.size() > 1) {
            return Optional.of("'" + EXCLUSIVE_OPTION_CODE + "' must stand alone");
        }
        return Optional.empty();
    }

    private static Optional<String> checkStringList(FieldValidation validation, Object value) {
        if (!(value instanceof List<?> list) || !list.stream().allMatch(String.class::isInstance)) {
            return Optional.of("list of strings expected");
        }
        for (Object item : list) {
            Optional<String> bad = checkPattern(validation, (String) item);
            if (bad.isPresent()) {
                return Optional.of("item '" + item + "' " + bad.get());
            }
        }
        return Optional.empty();
    }
}
