package com.example.caseintake.result;

import java.util.LinkedHashMap;
import java.util.Map;

/** One patient's case: every catalog field, in catalog order. */
public record CaseResult(int caseIndex, String label, Map<String, FieldResult> fields) {

    public CaseResult {
        fields = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("case_index", caseIndex);
        json.put("label", label);
        Map<String, Object> fieldJson = new LinkedHashMap<>();
        fields.forEach((id, result) -> fieldJson.put(id, result.toJson()));
        json.put("fields", fieldJson);
        return json;
    }
}
