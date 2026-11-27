package com.example.caseintake.llm;

import java.time.Duration;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Retries {@link LlmException.Retryable} failures with exponential backoff and jitter, honouring the provider's
 * retry hint when it is longer. Other failures, including an exhausted daily quota, are passed on at once.
 */
public final class RetryingLlmClient implements LlmClient {

    private static final Logger LOG = Logger.getLogger(RetryingLlmClient.class.getName());

    private final LlmClient delegate;
    private final int maxAttempts;
    private final Duration initialBackoff;
    private final Duration maxBackoff;
    private final Sleeper sleeper;
    private final Random random;

    public RetryingLlmClient(LlmClient delegate, int maxAttempts, Duration initialBackoff, Duration maxBackoff,
                             Sleeper sleeper, Random random) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.delegate = delegate;
        this.maxAttempts = maxAttempts;
        this.initialBackoff = initialBackoff;
        this.maxBackoff = maxBackoff;
        this.sleeper = sleeper;
        this.random = random;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        for (int attempt = 1; ; attempt++) {
            try {
                return delegate.complete(request);
            } catch (LlmException.Retryable e) {
                if (attempt >= maxAttempts) {
                    throw new LlmException("Giving up on " + request.documentId() + " after " + attempt + " attempts: " + e.getMessage(), e);
                }
                Duration wait = backoff(attempt, e);
                int next = attempt + 1;
                LOG.warning(() -> request.documentId() + ": " + e.getMessage() + "; attempt " + next + " in " + wait.toSeconds() + "s");
                try {
                    sleeper.sleep(wait);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new LlmException("Interrupted while waiting to retry", ie);
                }
            }
        }
    }

    Duration backoff(int attempt, LlmException.Retryable failure) {
        long exponential = initialBackoff.toMillis() << Math.min(attempt - 1, 20);
        long capped = Math.min(exponential, maxBackoff.toMillis());
        long jittered = capped / 2 + (long) (random.nextDouble() * (capped / 2.0));
        long hinted = failure.retryAfter().map(Duration::toMillis).orElse(0L);
        return Duration.ofMillis(Math.max(jittered, hinted));
    }
}
