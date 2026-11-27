package com.example.caseintake.llm;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Spaces calls evenly so that a requests-per-minute limit is never exceeded, including retries,
 * which pass through this decorator too.
 */
public final class RateLimitedLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final long minIntervalNanos;
    private final LongSupplier nanoClock;
    private final Sleeper sleeper;
    private long lastCallNanos;
    private boolean called;

    public RateLimitedLlmClient(LlmClient delegate, int requestsPerMinute, LongSupplier nanoClock, Sleeper sleeper) {
        if (requestsPerMinute <= 0) {
            throw new IllegalArgumentException("requests per minute must be positive");
        }
        this.delegate = delegate;
        this.minIntervalNanos = Duration.ofMinutes(1).toNanos() / requestsPerMinute;
        this.nanoClock = nanoClock;
        this.sleeper = sleeper;
    }

    @Override
    public synchronized LlmResponse complete(LlmRequest request) {
        if (called) {
            long wait = lastCallNanos + minIntervalNanos - nanoClock.getAsLong();
            if (wait > 0) {
                try {
                    sleeper.sleep(Duration.ofNanos(wait));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new LlmException("Interrupted while waiting for the rate limit", e);
                }
            }
        }
        called = true;
        lastCallNanos = nanoClock.getAsLong();
        return delegate.complete(request);
    }
}
