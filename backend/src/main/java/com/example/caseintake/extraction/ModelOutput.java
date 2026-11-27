package com.example.caseintake.extraction;

import com.example.caseintake.json.JsonFormatException;
import com.example.caseintake.json.JsonIo;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.caseintake.extraction.ModelOutputKeys.CASES;
import static com.example.caseintake.extraction.ModelOutputKeys.CONFIDENCE;
import static com.example.caseintake.extraction.ModelOutputKeys.CRITERIA;
import static com.example.caseintake.extraction.ModelOutputKeys.FIELDS;
import static com.example.caseintake.extraction.ModelOutputKeys.FLAGS;
import static com.example.caseintake.extraction.ModelOutputKeys.LABEL;
import static com.example.caseintake.extraction.ModelOutputKeys.NOTE;
import static com.example.caseintake.extraction.ModelOutputKeys.PRESENT;
import static com.example.caseintake.extraction.ModelOutputKeys.QUOTE;
import static com.example.caseintake.extraction.ModelOutputKeys.QUOTES;
import static com.example.caseintake.extraction.ModelOutputKeys.STATUS;
import static com.example.caseintake.extraction.ModelOutputKeys.VALUE;

/**
 * The model's answer, read as loosely typed data. Nothing here is trusted: statuses, values and confidences are
 * checked later by {@link FieldResolver}. Only a structure that cannot be read at all is rejected.
 */
public record ModelOutput(List<String> flags, List<ModelCase> cases) {

    public record ModelCase(String label, Map<MinimumCriterion, ModelCriterion> criteria, Map<String, ModelField> fields) {
    }

    public record ModelCriterion(boolean present, String quote) {
    }

    public record ModelField(Object status, Object value, Object confidence, List<String> quotes, String note) {
    }

    public static ModelOutput parse(String text) {
        Map<String, Object> root;
        try {
            root = JsonIo.parseObject(stripCodeFence(text));
        } catch (JsonFormatException e) {
            throw new ModelOutputException("The model did not return a JSON object: " + e.getMessage());
        }
        List<String> flags = new ArrayList<>();
        if (root.get(FLAGS) instanceof List<?> list) {
            list.stream().filter(String.class::isInstance).map(String.class::cast).forEach(flags::add);
        }
        if (!(root.get(CASES) instanceof List<?> rawCases)) {
            throw new ModelOutputException("The model output has no 'cases' list");
        }
        List<ModelCase> cases = new ArrayList<>();
        for (Object rawCase : rawCases) {
            if (!(rawCase instanceof Map<?, ?> c)) {
                throw new ModelOutputException("A case in the model output is not an object");
            }
            cases.add(parseCase(c));
        }
        return new ModelOutput(flags, cases);
    }

    private static ModelCase parseCase(Map<?, ?> c) {
        Map<MinimumCriterion, ModelCriterion> criteria = new EnumMap<>(MinimumCriterion.class);
        if (c.get(CRITERIA) instanceof Map<?, ?> rawCriteria) {
            for (MinimumCriterion criterion : MinimumCriterion.values()) {
                if (rawCriteria.get(criterion.jsonName()) instanceof Map<?, ?> m) {
                    criteria.put(criterion, new ModelCriterion(Boolean.TRUE.equals(m.get(PRESENT)), stringOrNull(m.get(QUOTE))));
                }
            }
        }
        Map<String, ModelField> fields = new LinkedHashMap<>();
        if (c.get(FIELDS) instanceof Map<?, ?> rawFields) {
            rawFields.forEach((id, raw) -> {
                if (raw instanceof Map<?, ?> f) {
                    fields.put(String.valueOf(id), new ModelField(f.get(STATUS), f.get(VALUE), f.get(CONFIDENCE),
                            quotes(f.get(QUOTES)), stringOrNull(f.get(NOTE))));
                }
            });
        }
        return new ModelCase(stringOrNull(c.get(LABEL)), criteria, fields);
    }

    private static List<String> quotes(Object raw) {
        if (raw instanceof String s) {
            return List.of(s);
        }
        if (raw instanceof List<?> list) {
            return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        }
        return List.of();
    }

    private static String stringOrNull(Object raw) {
        return raw instanceof String s && !s.isBlank() ? s : null;
    }

    static String stripCodeFence(String text) {
        String t = text.strip();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            int lastFence = t.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return t.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return t;
    }
}
