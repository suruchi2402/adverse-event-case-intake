package com.example.caseintake.llm;

import java.time.Duration;

/** Waiting, behind an interface so tests of rate limiting and retries run instantly. */
@FunctionalInterface
public interface Sleeper {

    Sleeper SYSTEM = duration -> Thread.sleep(duration.toMillis());

    void sleep(Duration duration) throws InterruptedException;
}
