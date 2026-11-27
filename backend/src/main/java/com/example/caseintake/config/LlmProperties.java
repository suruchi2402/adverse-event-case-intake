package com.example.caseintake.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Model settings from application.yml (prefix "llm"), overridable by environment variables such as LLM_PROVIDER
 * and GEMINI_MODEL. The API key itself is never a property: only the name of the environment variable that holds it.
 */
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(String provider, Gemini gemini, RateLimit rateLimit, Retry retry) {

    public record Gemini(String baseUrl, String model, String apiKeyEnv, double temperature, int maxOutputTokens, Duration timeout) {
    }

    public record RateLimit(int requestsPerMinute) {
    }

    public record Retry(int maxAttempts, Duration initialBackoff, Duration maxBackoff) {
    }
}
