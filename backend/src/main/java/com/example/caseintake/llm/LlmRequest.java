package com.example.caseintake.llm;

import java.util.Map;

/**
 * One extraction request. The document text is included in {@code userContent}; {@code documentSha256}
 * identifies the exact text so that a recording is never replayed against a changed document.
 */
public record LlmRequest(
        String documentId,
        String documentSha256,
        String systemInstruction,
        String userContent,
        Map<String, Object> responseSchema) {
}
