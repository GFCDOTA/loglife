package com.loglife.nutrition.api;

import com.loglife.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the total-estimation-failure path end to end: the primary provider is Ollama pointed
 * at a dead port (nothing listens on localhost:1), there is no fallback, so POST must surface a
 * structured 503 — and must do it fast (no transport retry), not after minutes of hanging.
 */
@TestPropertySource(properties = {
        "loglife.nutrition.estimation.provider=ollama",
        "loglife.nutrition.estimation.fallback-to-mock=false",
        "loglife.nutrition.estimation.ollama.base-url=http://localhost:1",
        "loglife.nutrition.estimation.ollama.connect-timeout=1s",
        "loglife.nutrition.estimation.ollama.timeout=2s"
})
class EstimationFailureApiIT extends AbstractPostgresIntegrationTest {

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @Value("${local.server.port}")
    private int port;

    @Test
    @SuppressWarnings("unchecked")
    void returns503WithStructuredErrorWhenEstimationCompletelyUnavailable() throws Exception {
        String body = """
                { "date": "2026-06-12", "mealType": "LUNCH", "description": "2 bifes e arroz" }
                """;
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/food-logs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        long start = System.nanoTime();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(response.statusCode()).isEqualTo(503);
        Map<String, Object> error = json.readValue(response.body(), Map.class);
        assertThat(((Number) error.get("status")).intValue()).isEqualTo(503);
        assertThat((String) error.get("message")).isNotBlank();
        // Transport failures must not be retried: a dead Ollama answers in well under the
        // old worst case of MAX_ATTEMPTS x read-timeout.
        assertThat(elapsedMs).isLessThan(10_000);
    }
}
