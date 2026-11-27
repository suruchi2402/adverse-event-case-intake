package com.example.caseintake.llm.fixture;

import com.example.caseintake.llm.LlmRequest;
import com.example.caseintake.llm.LlmResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/** One recorded model exchange: what was asked, for which exact document text, and what came back. */
public record Fixture(
        String documentId,
        String documentSha256,
        String model,
        String systemInstruction,
        String userContent,
        Map<String, Object> responseSchema,
        String responseText,
        Object rawResponse) {

    static final String KEY_DOCUMENT_ID = "document_id";
    static final String KEY_SHA = "document_sha256";
    static final String KEY_MODEL = "model";
    static final String KEY_REQUEST = "request";
    static final String KEY_SYSTEM = "system_instruction";
    static final String KEY_USER = "user_content";
    static final String KEY_SCHEMA = "response_schema";
    static final String KEY_RESPONSE = "response";
    static final String KEY_TEXT = "text";
    static final String KEY_RAW = "raw";

    public static Fixture of(LlmRequest request, LlmResponse response, Object rawResponse) {
        return new Fixture(request.documentId(), request.documentSha256(), response.model(), request.systemInstruction(),
                request.userContent(), request.responseSchema(), response.text(), rawResponse);
    }

    public LlmResponse toResponse() {
        return new LlmResponse(model, responseText, null);
    }

    Map<String, Object> toJson() {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put(KEY_SYSTEM, systemInstruction);
        req.put(KEY_USER, userContent);
        req.put(KEY_SCHEMA, responseSchema);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put(KEY_TEXT, responseText);
        resp.put(KEY_RAW, rawResponse);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put(KEY_DOCUMENT_ID, documentId);
        json.put(KEY_SHA, documentSha256);
        json.put(KEY_MODEL, model);
        json.put(KEY_REQUEST, req);
        json.put(KEY_RESPONSE, resp);
        return json;
    }

    @SuppressWarnings("unchecked")
    static Fixture fromJson(Map<String, Object> json) {
        Map<String, Object> req = json.get(KEY_REQUEST) instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        Map<String, Object> resp = json.get(KEY_RESPONSE) instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        if (!(resp.get(KEY_TEXT) instanceof String text)) {
            throw new FixtureException("Fixture for " + json.get(KEY_DOCUMENT_ID) + " has no response text");
        }
        return new Fixture(
                (String) json.get(KEY_DOCUMENT_ID),
                (String) json.get(KEY_SHA),
                (String) json.get(KEY_MODEL),
                (String) req.get(KEY_SYSTEM),
                (String) req.get(KEY_USER),
                req.get(KEY_SCHEMA) instanceof Map<?, ?> s ? (Map<String, Object>) s : null,
                text,
                resp.get(KEY_RAW));
    }
}
