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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trend end to end: 7 buckets with zero-filled holes and an average over logged days only.
 */
class NutritionTrendApiIT extends AbstractPostgresIntegrationTest {

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @Value("${local.server.port}")
    private int port;

    @Test
    @SuppressWarnings("unchecked")
    void sevenBucketsZeroFilledAndHonestAverage() throws Exception {
        // 2000 kcal six days ago, 1500 today, nothing in between (manual = deterministic).
        postManual("2026-06-06", 2000);
        postManual("2026-06-12", 1500);

        HttpResponse<String> response = send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port
                                + "/api/v1/nutrition/trend?date=2026-06-12&days=7"))
                .GET().build());

        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> body = json.readValue(response.body(), Map.class);
        List<Map<String, Object>> days = (List<Map<String, Object>>) body.get("days");
        assertThat(days).hasSize(7);
        assertThat(days.get(0).get("date")).isEqualTo("2026-06-06");
        assertThat(((Number) days.get(0).get("calories")).doubleValue()).isEqualTo(2000.0);
        assertThat(((Number) days.get(3).get("calories")).doubleValue()).isEqualTo(0.0);
        assertThat(days.get(6).get("date")).isEqualTo("2026-06-12");
        assertThat(((Number) body.get("daysWithLogs")).intValue()).isEqualTo(2);
        assertThat(((Number) body.get("averageCalories")).doubleValue()).isEqualTo(1750.0);
    }

    private void postManual(String date, int calories) throws Exception {
        HttpResponse<String> created = send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/food-logs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "date": "%s", "mealType": "LUNCH", "description": "dia de %d kcal",
                          "nutrition": { "calories": %d, "proteinGrams": 1, "carbsGrams": 1, "fatGrams": 1 }
                        }
                        """.formatted(date, calories, calories), StandardCharsets.UTF_8)).build());
        assertThat(created.statusCode()).isEqualTo(201);
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
