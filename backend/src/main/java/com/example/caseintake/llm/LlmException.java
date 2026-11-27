package com.example.caseintake.llm;

import java.time.Duration;
import java.util.Optional;

/** A model call failed. {@link Retryable} failures may succeed if repeated; others will not. */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Rate limited or a transient server error. {@code retryAfter} is the provider's hint, when it gave one. */
    public static class Retryable extends LlmException {
        private final Duration retryAfter;

        public Retryable(String message, Duration retryAfter) {
            super(message);
            this.retryAfter = retryAfter;
        }

        public Optional<Duration> retryAfter() {
            return Optional.ofNullable(retryAfter);
        }
    }

    /** A daily quota is used up. Retrying today is pointless; the run stops and can resume later from fixtures. */
    public static class QuotaExhausted extends LlmException {
        public QuotaExhausted(String message) {
            super(message);
        }
    }
}
