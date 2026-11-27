package com.example.caseintake.result;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** The status of one field in one case, as the output schema defines it. */
public enum FieldStatus {
    EXTRACTED("extracted"),
    UNKNOWN("unknown"),
    CONFLICT("conflict");

    private final String jsonName;

    FieldStatus(String jsonName) {
        this.jsonName = jsonName;
    }

    public String jsonName() {
        return jsonName;
    }

    public static Optional<FieldStatus> fromJson(Object name) {
        return Arrays.stream(values()).filter(s -> s.jsonName.equals(name)).findFirst();
    }

    public static Set<String> jsonNames() {
        return Arrays.stream(values()).map(FieldStatus::jsonName).collect(Collectors.toSet());
    }
}
