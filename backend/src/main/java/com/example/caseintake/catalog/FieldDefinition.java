package com.example.caseintake.catalog;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** One field of the catalog, exactly as the catalog defines it. */
public record FieldDefinition(
        String id,
        String label,
        String sectionId,
        FieldType type,
        List<FieldOption> options,
        FieldValidation validation,
        String guidance) {

    public FieldDefinition {
        options = List.copyOf(options);
    }

    public List<String> optionCodes() {
        return options.stream().map(FieldOption::code).toList();
    }

    public Optional<FieldOption> option(String code) {
        return options.stream().filter(o -> o.code().equals(code)).findFirst();
    }

    public Optional<String> guidanceText() {
        return Optional.ofNullable(guidance).filter(g -> !g.isBlank());
    }

    /** One selectable value of a coded or coded_multi field. */
    public record FieldOption(String code, String label) {
    }

    /** The validation keys the catalog may use: pattern, min, max. Each is optional. */
    public record FieldValidation(Pattern pattern, Double min, Double max) {
        public static final FieldValidation NONE = new FieldValidation(null, null, null);

        public Optional<Pattern> patternRule() {
            return Optional.ofNullable(pattern);
        }

        public Optional<Double> minRule() {
            return Optional.ofNullable(min);
        }

        public Optional<Double> maxRule() {
            return Optional.ofNullable(max);
        }
    }
}
