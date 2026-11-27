package com.example.caseintake.result;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The output file for one document, in the shape of schema/extraction.schema.json. */
public record DocumentResult(String documentId, String catalogVersion, String model, List<String> flags, List<CaseResult> cases) {

    public DocumentResult {
        flags = List.copyOf(flags);
        cases = List.copyOf(cases);
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("document_id", documentId);
        json.put("catalog_version", catalogVersion);
        if (model != null) {
            json.put("model", model);
        }
        json.put("flags", flags);
        json.put("cases", cases.stream().map(CaseResult::toJson).toList());
        return json;
    }
}
