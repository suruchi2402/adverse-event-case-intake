package com.example.caseintake.result;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A quote from the source text and its position. Offsets count Unicode code points, start inclusive, end exclusive.
 * The quote is always the exact slice of the source, so the checker's span test holds by construction.
 */
public record Provenance(String quote, int start, int end, Integer page) {

    public Optional<Integer> pageNumber() {
        return Optional.ofNullable(page);
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("quote", quote);
        json.put("start", start);
        json.put("end", end);
        pageNumber().ifPresent(p -> json.put("page", p));
        return json;
    }
}
