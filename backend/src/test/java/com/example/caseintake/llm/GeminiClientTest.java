package com.example.caseintake.llm;

import com.example.caseintake.TestData;
import com.example.caseintake.json.JsonIo;
import com.example.caseintake.llm.GeminiClient.GeminiSettings;
import com.example.caseintake.llm.fixture.FixtureStore;
import com.example.caseintake.llm.fixture.RecordingLlmClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the real HTTP client against a local fake of the Gemini endpoint. No network beyond localhost. */
class GeminiClientTest {

    private static final String KEY = "AIza" + "x".repeat(35);
    private static final String OK_BODY = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"flags\\\":[],\\\"cases\\\":[]}\"}]},"
            + "\"finishReason\":\"STOP\"}],\"modelVersion\":\"fake\"}";

    private HttpServer server;
    private final Deque<int[]> statuses = new ArrayDeque<>();
    private final Deque<String> bodies = new ArrayDeque<>();
    private final List<String> seenKeys = new ArrayList<>();
    private final List<String> seenPaths = new ArrayList<>();
    private final List<String> seenBodies = new ArrayList<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            seenKeys.add(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            seenPaths.add(exchange.getRequestURI().toString());
            seenBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int status = statuses.isEmpty() ? 200 : statuses.poll()[0];
            byte[] body = (bodies.isEmpty() ? OK_BODY : bodies.poll()).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private GeminiClient client() {
        String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta";
        return new GeminiClient(HttpClient.newHttpClient(), new GeminiSettings(base, "fake-model", 0, 1024, Duration.ofSeconds(5)), KEY);
    }

    private static LlmRequest request() {
        return new LlmRequest("D01", "sha", "system", "user", Map.of("type", "OBJECT"));
    }

    private void respond(int status, String body) {
        statuses.add(new int[]{status});
        bodies.add(body);
    }

    @Test
    void sendsTheKeyOnlyInAHeaderAndReadsTheText() {
        LlmResponse response = client().complete(request());
        assertEquals("{\"flags\":[],\"cases\":[]}", response.text());
        assertEquals("gemini/fake-model", response.model());
        assertEquals(KEY, seenKeys.get(0));
        assertEquals("/v1beta/models/fake-model:generateContent", seenPaths.get(0));
        assertFalse(seenPaths.get(0).contains(KEY));
        Map<String, Object> body = JsonIo.parseObject(seenBodies.get(0));
        assertTrue(body.containsKey("systemInstruction"));
        assertEquals("application/json", ((Map<?, ?>) body.get("generationConfig")).get("responseMimeType"));
    }

    @Test
    void rateLimitIsRetryableWithTheServersHint() {
        respond(429, "{\"error\":{\"code\":429,\"message\":\"slow down\",\"details\":[{\"@type\":\"type.googleapis.com/google.rpc.RetryInfo\",\"retryDelay\":\"17s\"}]}}");
        LlmException.Retryable e = assertThrows(LlmException.Retryable.class, () -> client().complete(request()));
        assertEquals(Duration.ofSeconds(17), e.retryAfter().orElseThrow());
    }

    @Test
    void anExhaustedDailyQuotaIsNotRetried() {
        respond(429, "{\"error\":{\"code\":429,\"message\":\"quota\",\"details\":[{\"violations\":[{\"quotaId\":\"GenerateRequestsPerDayPerProjectPerModel-FreeTier\"}]}]}}");
        assertThrows(LlmException.QuotaExhausted.class, () -> client().complete(request()));
    }

    @Test
    void aTruncatedAnswerIsAnError() {
        respond(200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\"}]},\"finishReason\":\"MAX_TOKENS\"}]}");
        LlmException e = assertThrows(LlmException.class, () -> client().complete(request()));
        assertTrue(e.getMessage().contains("MAX_TOKENS"));
    }

    @Test
    void retriesThenRecordsWithoutTheKey() throws IOException {
        respond(503, "{\"error\":{\"message\":\"overloaded\"}}");
        respond(200, OK_BODY.replace("fake", KEY)); // a key echoed anywhere in the response must not reach the file
        List<Duration> waits = new ArrayList<>();
        LlmClient retrying = new RetryingLlmClient(client(), 3, Duration.ofSeconds(2), Duration.ofSeconds(60), waits::add, new Random(1));
        Path fixtures = TestData.tempDir();
        LlmClient recording = new RecordingLlmClient(retrying, new FixtureStore(fixtures, List.of(KEY)), true);

        recording.complete(request());

        assertEquals(1, waits.size());
        String recorded = Files.readString(fixtures.resolve("D01.json"));
        assertFalse(recorded.contains(KEY));
        assertFalse(FixtureStore.containsGoogleApiKey(recorded));
        assertTrue(recorded.contains(FixtureStore.REDACTED));

        recording.complete(request()); // second call is answered from the recording
        assertEquals(2, seenKeys.size());
    }

    @Test
    void retryGivesUpAfterMaxAttempts() {
        LlmClient alwaysBusy = r -> {
            throw new LlmException.Retryable("busy", null);
        };
        List<Duration> waits = new ArrayList<>();
        LlmClient retrying = new RetryingLlmClient(alwaysBusy, 3, Duration.ofSeconds(1), Duration.ofSeconds(4), waits::add, new Random(1));
        assertThrows(LlmException.class, () -> retrying.complete(request()));
        assertEquals(2, waits.size());
        assertTrue(waits.stream().allMatch(w -> w.compareTo(Duration.ofSeconds(4)) <= 0));
    }

    @Test
    void rateLimiterSpacesCalls() {
        long[] now = {0};
        List<Duration> waits = new ArrayList<>();
        LlmClient limited = new RateLimitedLlmClient(r -> new LlmResponse("m", "{}", null), 6, () -> now[0],
                d -> {
                    waits.add(d);
                    now[0] += d.toNanos();
                });
        limited.complete(request());
        now[0] += Duration.ofSeconds(4).toNanos();
        limited.complete(request());
        assertEquals(List.of(Duration.ofSeconds(6)), waits);
    }
}
