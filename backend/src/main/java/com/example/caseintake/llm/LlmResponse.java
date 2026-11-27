package com.example.caseintake.llm;

/**
 * The model's answer. {@code model} is written as provider/model, as the output schema asks.
 * {@code rawResponse} is the provider's full response body, kept in fixtures for audit; it may be null in replay.
 */
public record LlmResponse(String model, String text, String rawResponse) {
}
