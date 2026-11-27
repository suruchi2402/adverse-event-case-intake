package com.example.caseintake.llm;

import com.example.caseintake.json.JsonFormatException;
import com.example.caseintake.json.JsonIo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Google Gemini over its REST API (generateContent), with JSON output constrained by a response schema.
 * The API key travels only in the x-goog-api-key header, never in the URL, so it cannot reach a recording or a log.
 */
public final class GeminiClient implements LlmClient {

    public static final String PROVIDER = "gemini";

    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final String FINISH_OK = "STOP";
    private static final String DAILY_QUOTA_MARKER = "PerDay";
    private static final String RETRY_INFO_TYPE = "RetryInfo";
    private static final Set<Integer> RETRYABLE_STATUS = Set.of(408, 429, 500, 502, 503, 504);

    private final HttpClient http;
    private final GeminiSettings settings;
    private final String apiKey;

    public record GeminiSettings(String baseUrl, String model, double temperature, int maxOutputTokens, Duration timeout) {
    }

    public GeminiClient(HttpClient http, GeminiSettings settings, String apiKey) {
        if (settings.model() == null || settings.model().isBlank()) {
            throw new IllegalArgumentException("No Gemini model configured: set GEMINI_MODEL (copy the exact id from AI Studio)");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("No Gemini API key in the environment");
        }
        this.http = http;
        this.settings = settings;
        this.apiKey = apiKey;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint())
                .timeout(settings.timeout())
                .header("Content-Type", "application/json")
                .header(API_KEY_HEADER, apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(JsonIo.compact(requestBody(request))))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new LlmException.Retryable("Gemini request failed: " + e.getMessage(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted while calling Gemini", e);
        }
        if (response.statusCode() != 200) {
            throw errorFor(response);
        }
        return new LlmResponse(PROVIDER + "/" + settings.model(), responseText(response.body()), response.body());
    }

    private URI endpoint() {
        return URI.create(settings.baseUrl().replaceAll("/+$", "") + "/models/" + settings.model() + ":generateContent");
    }

    Map<String, Object> requestBody(LlmRequest request) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", settings.temperature());
        generationConfig.put("maxOutputTokens", settings.maxOutputTokens());
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", request.responseSchema());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", request.systemInstruction()))));
        body.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", request.userContent())))));
        body.put("generationConfig", generationConfig);
        return body;
    }

    static String responseText(String body) {
        Map<String, Object> json;
        try {
            json = JsonIo.parseObject(body);
        } catch (JsonFormatException e) {
            throw new LlmException("Gemini returned a body that is not JSON", e);
        }
        if (json.get("promptFeedback") instanceof Map<?, ?> feedback && feedback.get("blockReason") != null) {
            throw new LlmException("Gemini blocked the prompt: " + feedback.get("blockReason"));
        }
        if (!(json.get("candidates") instanceof List<?> candidates) || candidates.isEmpty()
                || !(candidates.get(0) instanceof Map<?, ?> candidate)) {
            throw new LlmException("Gemini returned no candidates");
        }
        Object finish = candidate.get("finishReason");
        if (finish != null && !FINISH_OK.equals(finish)) {
            throw new LlmException("Gemini stopped with finishReason " + finish);
        }
        List<String> texts = new ArrayList<>();
        if (candidate.get("content") instanceof Map<?, ?> content && content.get("parts") instanceof List<?> parts) {
            for (Object part : parts) {
                if (part instanceof Map<?, ?> p && p.get("text") instanceof String t && !Boolean.TRUE.equals(p.get("thought"))) {
                    texts.add(t);
                }
            }
        }
        if (texts.isEmpty()) {
            throw new LlmException("Gemini returned a candidate without text");
        }
        return String.join("", texts);
    }

    private static LlmException errorFor(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body() == null ? "" : response.body();
        String summary = "Gemini HTTP " + status + ": " + errorMessage(body);
        if (status == 429 && body.contains(DAILY_QUOTA_MARKER)) {
            return new LlmException.QuotaExhausted(summary);
        }
        if (RETRYABLE_STATUS.contains(status)) {
            Duration hint = retryDelay(body).or(() -> retryAfterHeader(response)).orElse(null);
            return new LlmException.Retryable(summary, hint);
        }
        return new LlmException(summary);
    }

    private static String errorMessage(String body) {
        try {
            if (JsonIo.parseObject(body).get("error") instanceof Map<?, ?> error && error.get("message") instanceof String m) {
                return m;
            }
        } catch (JsonFormatException ignored) {
            // Not a JSON error body: fall through to the raw text below.
        }
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }

    /** The RetryInfo detail of a Google API error, for example {"retryDelay": "17s"}. */
    static Optional<Duration> retryDelay(String body) {
        try {
            if (JsonIo.parseObject(body).get("error") instanceof Map<?, ?> error && error.get("details") instanceof List<?> details) {
                for (Object detail : details) {
                    if (detail instanceof Map<?, ?> d && String.valueOf(d.get("@type")).endsWith(RETRY_INFO_TYPE)
                            && d.get("retryDelay") instanceof String delay) {
                        return parseSeconds(delay);
                    }
                }
            }
        } catch (JsonFormatException ignored) {
            // No structured hint; the caller falls back to the Retry-After header or its own backoff.
        }
        return Optional.empty();
    }

    private static Optional<Duration> retryAfterHeader(HttpResponse<String> response) {
        return response.headers().firstValue("Retry-After").flatMap(GeminiClient::parseSeconds);
    }

    private static Optional<Duration> parseSeconds(String value) {
        try {
            double seconds = Double.parseDouble(value.strip().replaceAll("s$", ""));
            return Optional.of(Duration.ofMillis((long) Math.ceil(seconds * 1000)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
