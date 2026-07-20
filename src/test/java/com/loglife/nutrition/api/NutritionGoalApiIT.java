package com.loglife.nutrition.api;

import com.loglife.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Goal lifecycle end to end: unset goal is a normal state (204), PUT is an upsert, and the daily
 * summary scores the day against the stored goal.
 */
class NutritionGoalApiIT extends AbstractPostgresIntegrationTest {

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @Value("${local.server.port}")
    private int port;

    private String base() {
        return "http://localhost:" + port;
    }

    @Test
    void unsetGoalReturnsNoContent() throws Exception {
        HttpResponse<String> response =
                send(request("/api/v1/nutrition/goal").GET().build());

        assertThat(response.statusCode()).isEqualTo(204);
    }

    @Test
    @SuppressWarnings("unchecked")
    void putReadAndReplaceGoal() throws Exception {
        HttpResponse<String> put = send(request("/api/v1/nutrition/goal")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("""
                        { "calories": 2000, "proteinGrams": 150 }
                        """, StandardCharsets.UTF_8)).build());
        assertThat(put.statusCode()).isEqualTo(200);

        HttpResponse<String> get = send(request("/api/v1/nutrition/goal").GET().build());
        assertThat(get.statusCode()).isEqualTo(200);
        Map<String, Object> goal = json.readValue(get.body(), Map.class);
        assertThat(((Number) goal.get("calories")).doubleValue()).isEqualTo(2000.0);
        assertThat(((Number) goal.get("proteinGrams")).doubleValue()).isEqualTo(150.0);
        assertThat(goal.get("carbsGrams")).isNull();

        // PUT replaces (upsert on the single row), never accumulates.
        HttpResponse<String> replace = send(request("/api/v1/nutrition/goal")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("""
                        { "calories": 1800 }
                        """, StandardCharsets.UTF_8)).build());
        assertThat(replace.statusCode()).isEqualTo(200);
        Map<String, Object> replaced = json.readValue(replace.body(), Map.class);
        assertThat(((Number) replaced.get("calories")).doubleValue()).isEqualTo(1800.0);
        assertThat(replaced.get("proteinGrams")).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void dailySummaryScoresAgainstTheGoal() throws Exception {
        send(request("/api/v1/nutrition/goal")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("""
                        { "calories": 2000 }
                        """, StandardCharsets.UTF_8)).build());

        // one manual log of 500 kcal so the numbers are deterministic (no estimator involved)
        send(request("/api/v1/food-logs")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "date": "2026-06-12", "mealType": "LUNCH", "description": "marmita",
                          "nutrition": { "calories": 500, "proteinGrams": 30, "carbsGrams": 50, "fatGrams": 15 }
                        }
                        """, StandardCharsets.UTF_8)).build());

        HttpResponse<String> summary =
                send(request("/api/v1/nutrition/daily-summary?date=2026-06-12").GET().build());
        Map<String, Object> body = json.readValue(summary.body(), Map.class);
        Map<String, Object> goal = (Map<String, Object>) body.get("goal");
        assertThat(goal).isNotNull();
        assertThat(((Number) goal.get("calories")).doubleValue()).isEqualTo(2000.0);
        assertThat(((Number) goal.get("remainingCalories")).doubleValue()).isEqualTo(1500.0);
        assertThat(((Number) goal.get("percentOfCalories")).intValue()).isEqualTo(25);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(base() + path));
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
