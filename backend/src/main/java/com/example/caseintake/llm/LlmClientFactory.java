package com.example.caseintake.llm;

import com.example.caseintake.config.LlmProperties;
import com.example.caseintake.llm.GeminiClient.GeminiSettings;
import com.example.caseintake.llm.fixture.FixtureStore;
import com.example.caseintake.llm.fixture.RecordingLlmClient;
import com.example.caseintake.llm.fixture.ReplayLlmClient;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Builds the model client from configuration. Switching provider is a configuration change (llm.provider).
 * Live providers are always wrapped as: record and reuse recordings, then retry, then rate limit, then the provider.
 * To add a provider, implement {@link LlmClient} and add a case to {@link #live}.
 */
public final class LlmClientFactory {

    public static final String REPLAY = "replay";

    private LlmClientFactory() {
    }

    public static LlmClient create(LlmProperties properties, Path fixturesDir, boolean refresh, UnaryOperator<String> environment) {
        String provider = properties.provider() == null ? "" : properties.provider().strip();
        if (provider.equals(REPLAY)) {
            return new ReplayLlmClient(new FixtureStore(fixturesDir, List.of()));
        }
        String apiKey = environment.apply(properties.gemini().apiKeyEnv());
        FixtureStore store = new FixtureStore(fixturesDir, apiKey == null ? List.of() : List.of(apiKey));
        LlmClient limited = new RateLimitedLlmClient(live(provider, properties, apiKey),
                properties.rateLimit().requestsPerMinute(), System::nanoTime, Sleeper.SYSTEM);
        LlmClient retrying = new RetryingLlmClient(limited, properties.retry().maxAttempts(),
                properties.retry().initialBackoff(), properties.retry().maxBackoff(), Sleeper.SYSTEM, new SecureRandom());
        return new RecordingLlmClient(retrying, store, !refresh);
    }

    private static LlmClient live(String provider, LlmProperties properties, String apiKey) {
        return switch (provider) {
            case GeminiClient.PROVIDER -> {
                LlmProperties.Gemini g = properties.gemini();
                if (apiKey == null || apiKey.isBlank()) {
                    throw new IllegalStateException("Set the environment variable " + g.apiKeyEnv()
                            + " to call " + provider + ", or use --provider=" + REPLAY + " to run from fixtures.");
                }
                HttpClient http = HttpClient.newBuilder().connectTimeout(g.timeout()).build();
                yield new GeminiClient(http, new GeminiSettings(g.baseUrl(), g.model(), g.temperature(), g.maxOutputTokens(), g.timeout()), apiKey);
            }
            default -> throw new IllegalStateException("Unknown llm.provider '" + provider + "'. Supported: "
                    + GeminiClient.PROVIDER + ", " + REPLAY + ".");
        };
    }
}
