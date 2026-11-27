package com.example.caseintake.validation;

import com.example.caseintake.catalog.FieldDefinition;
import com.example.caseintake.catalog.FieldDefinition.FieldOption;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Brings a model value into the JSON shape its field type expects, without changing its meaning:
 * trims strings, turns "54" into 54, maps an option label or a differently cased code to the code, removes duplicates.
 * A value that cannot be brought into shape raises {@link InvalidValueException}; it is never replaced by a guess.
 */
public final class ValueNormalizer {

    public Object normalize(FieldDefinition field, Object raw) {
        if (raw == null) {
            throw new InvalidValueException("no value");
        }
        return switch (field.type()) {
            case STRING, DATE -> nonBlankString(raw);
            case INTEGER -> toInteger(raw);
            case NUMBER -> toNumber(raw);
            case CODED -> toCode(field, raw);
            case CODED_MULTI -> toCodes(field, raw);
            case STRING_LIST -> toStringList(raw);
        };
    }

    private static String nonBlankString(Object raw) {
        if (!(raw instanceof String s)) {
            throw new InvalidValueException("string expected, got " + raw);
        }
        String trimmed = s.strip();
        if (trimmed.isEmpty()) {
            throw new InvalidValueException("empty string");
        }
        return trimmed;
    }

    private static Object toInteger(Object raw) {
        BigDecimal number = decimal(raw);
        try {
            return number.longValueExact();
        } catch (ArithmeticException e) {
            throw new InvalidValueException("integer expected, got " + raw);
        }
    }

    private static Object toNumber(Object raw) {
        BigDecimal number = decimal(raw).stripTrailingZeros();
        if (number.scale() <= 0) {
            try {
                return number.longValueExact();
            } catch (ArithmeticException e) {
                throw new InvalidValueException("number out of range: " + raw);
            }
        }
        return number.doubleValue();
    }

    private static BigDecimal decimal(Object raw) {
        if (raw instanceof Boolean) {
            throw new InvalidValueException("number expected, got " + raw);
        }
        if (raw instanceof Number n) {
            double d = n.doubleValue();
            if (!Double.isFinite(d)) {
                throw new InvalidValueException("finite number expected");
            }
            return new BigDecimal(n.toString());
        }
        if (raw instanceof String s) {
            try {
                return new BigDecimal(s.strip());
            } catch (NumberFormatException e) {
                throw new InvalidValueException("number expected, got '" + s + "'");
            }
        }
        throw new InvalidValueException("number expected, got " + raw);
    }

    private static String toCode(FieldDefinition field, Object raw) {
        String text = nonBlankString(raw);
        return matchOption(field, text).map(FieldOption::code)
                .orElseThrow(() -> new InvalidValueException("not an option code: '" + text + "'"));
    }

    private static List<String> toCodes(FieldDefinition field, Object raw) {
        List<?> items = raw instanceof List<?> list ? list : List.of(raw);
        Set<String> codes = new LinkedHashSet<>();
        for (Object item : items) {
            codes.add(toCode(field, item));
        }
        // Catalog order, so that the same set always serialises the same way.
        return field.optionCodes().stream().filter(codes::contains).toList();
    }

    private static Optional<FieldOption> matchOption(FieldDefinition field, String text) {
        Optional<FieldOption> exact = field.option(text);
        if (exact.isPresent()) {
            return exact;
        }
        String folded = text.toLowerCase(Locale.ROOT);
        return field.options().stream()
                .filter(o -> o.code().toLowerCase(Locale.ROOT).equals(folded) || o.label().toLowerCase(Locale.ROOT).equals(folded))
                .findFirst();
    }

    private static List<String> toStringList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            throw new InvalidValueException("list expected, got " + raw);
        }
        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String s)) {
                throw new InvalidValueException("list of strings expected, got item " + item);
            }
            String trimmed = s.strip();
            if (!trimmed.isEmpty() && seen.add(trimmed.toLowerCase(Locale.ROOT))) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
