package com.example.caseintake.result;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Document-level flags with a fixed meaning. Free-text flags (for example why a case was dropped) are also allowed
 * by the schema and are written as plain strings.
 */
public enum DocumentFlag {
    NO_ADVERSE_EVENT("no_adverse_event", true),
    MULTIPLE_PATIENTS("multiple_patients", false),
    UNSUPPORTED_FORMAT("unsupported_format", false),
    POSSIBLE_DUPLICATE("possible_duplicate", true),
    EMBEDDED_INSTRUCTIONS("embedded_instructions", true);

    private final String jsonName;
    private final boolean modelMayRaise;

    DocumentFlag(String jsonName, boolean modelMayRaise) {
        this.jsonName = jsonName;
        this.modelMayRaise = modelMayRaise;
    }

    public String jsonName() {
        return jsonName;
    }

    /** Flags the model may report. The others are set by the pipeline itself, from facts it can check. */
    public static List<String> modelFlagNames() {
        return Arrays.stream(values()).filter(f -> f.modelMayRaise).map(DocumentFlag::jsonName).toList();
    }

    public static Optional<DocumentFlag> fromJson(Object name) {
        return Arrays.stream(values()).filter(f -> f.jsonName.equals(name)).findFirst();
    }
}
