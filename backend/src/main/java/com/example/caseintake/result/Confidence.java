package com.example.caseintake.result;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Confidence of an extracted value, ordered from weakest to strongest. */
public enum Confidence {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String jsonName;

    Confidence(String jsonName) {
        this.jsonName = jsonName;
    }

    public String jsonName() {
        return jsonName;
    }

    /** The weaker of this confidence and the cap. */
    public Confidence cappedAt(Confidence cap) {
        return compareTo(cap) <= 0 ? this : cap;
    }

    public static Optional<Confidence> fromJson(Object name) {
        return Arrays.stream(values()).filter(c -> c.jsonName.equals(name)).findFirst();
    }

    public static Set<String> jsonNames() {
        return Arrays.stream(values()).map(Confidence::jsonName).collect(Collectors.toSet());
    }
}
