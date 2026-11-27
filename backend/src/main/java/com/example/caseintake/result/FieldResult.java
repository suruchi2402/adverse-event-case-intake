package com.example.caseintake.result;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The final, validated result for one field of one case. Only EXTRACTED carries a value. */
public record FieldResult(FieldStatus status, Object value, Confidence confidence, List<Provenance> provenance, String note) {

    public FieldResult {
        provenance = List.copyOf(provenance);
        if (status == FieldStatus.EXTRACTED && (value == null || confidence == null || provenance.isEmpty())) {
            throw new IllegalArgumentException("An extracted field needs a value, a confidence and provenance");
        }
        if (status != FieldStatus.EXTRACTED && value != null) {
            throw new IllegalArgumentException("Only an extracted field carries a value");
        }
    }

    public static FieldResult extracted(Object value, Confidence confidence, List<Provenance> provenance, String note) {
        return new FieldResult(FieldStatus.EXTRACTED, value, confidence, provenance, note);
    }

    public static FieldResult unknown(String note) {
        return new FieldResult(FieldStatus.UNKNOWN, null, null, List.of(), note);
    }

    public static FieldResult conflict(String note) {
        return new FieldResult(FieldStatus.CONFLICT, null, null, List.of(), note);
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("status", status.jsonName());
        if (status == FieldStatus.EXTRACTED) {
            json.put("value", value);
            json.put("confidence", confidence.jsonName());
            json.put("provenance", provenance.stream().map(Provenance::toJson).toList());
        }
        if (note != null && !note.isBlank()) {
            json.put("note", note);
        }
        return json;
    }
}
