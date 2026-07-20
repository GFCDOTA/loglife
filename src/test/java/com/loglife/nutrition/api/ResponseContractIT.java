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
 * Pins the COMPLETE key set of every public response shape. A key renamed or dropped breaks the
 * PWA (and any shortcut automation) silently — this is the tripwire.
 */
class ResponseContractIT extends AbstractPostgresIntegrationTest {

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @Value("${local.server.port}")
    private int port;

    private String base() {
        return "http://localhost:" + port;
    }

    @Test
    @SuppressWarnings("unchecked")
    void foodLogAndSummaryAndErrorKeySetsAreComplete() throws Exception {
        // goal first so the summary carries the goal block
        send(HttpRequest.newBuilder(URI.create(base() + "/api/v1/nutrition/goal"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"calories\":2000}", StandardCharsets.UTF_8))
                .build());
        HttpResponse<String> created = send(HttpRequest.newBuilder(URI.create(base() + "/api/v1/food-logs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "date": "2026-06-12", "mealType": "LUNCH", "description": "marmita",
                          "nutrition": { "calories": 500, "proteinGrams": 30, "carbsGrams": 50, "fatGrams": 15 }
                        }
                        """, StandardCharsets.UTF_8)).build());

        Map<String, Object> log = (Map<String, Object>) json.readValue(created.body(), List.class).get(0);
        assertThat(log.keySet()).containsExactlyInAnyOrder(
                "id", "date", "mealType", "descriptionOriginal", "normalizedFoodName",
                "quantity", "unit", "calories", "proteinGrams", "carbsGrams", "fatGrams",
                "confidence", "source", "notes", "explanation", "createdAt", "updatedAt");

        HttpResponse<String> summary = send(HttpRequest.newBuilder(
                URI.create(base() + "/api/v1/nutrition/daily-summary?date=2026-06-12")).GET().build());
        Map<String, Object> summaryJson = json.readValue(summary.body(), Map.class);
        assertThat(summaryJson.keySet()).containsExactlyInAnyOrder(
                "date", "totalCalories", "totalProteinGrams", "totalCarbsGrams", "totalFatGrams",
                "totalLogs", "logsByMealType", "goal");
        Map<String, Object> goal = (Map<String, Object>) summaryJson.get("goal");
        assertThat(goal.keySet()).containsExactlyInAnyOrder(
                "calories", "proteinGrams", "carbsGrams", "fatGrams",
                "remainingCalories", "percentOfCalories");

        HttpResponse<String> trend = send(HttpRequest.newBuilder(
                URI.create(base() + "/api/v1/nutrition/trend?date=2026-06-12&days=7")).GET().build());
        Map<String, Object> trendJson = json.readValue(trend.body(), Map.class);
        assertThat(trendJson.keySet()).containsExactlyInAnyOrder(
                "days", "daysWithLogs", "averageCalories");
        Map<String, Object> bucket = (Map<String, Object>) ((List<Object>) trendJson.get("days")).get(0);
        assertThat(bucket.keySet()).containsExactlyInAnyOrder(
                "date", "totalLogs", "calories", "proteinGrams", "carbsGrams", "fatGrams");

        HttpResponse<String> frequent = send(HttpRequest.newBuilder(
                URI.create(base() + "/api/v1/food-logs/frequent?date=2026-06-12")).GET().build());
        Map<String, Object> chip = (Map<String, Object>) json.readValue(frequent.body(), List.class).get(0);
        assertThat(chip.keySet()).containsExactlyInAnyOrder(
                "logId", "name", "timesLogged", "mealType", "calories", "proteinGrams",
                "carbsGrams", "fatGrams", "quantity", "unit", "source");

        HttpResponse<String> error = send(HttpRequest.newBuilder(
                URI.create(base() + "/api/v1/food-logs?date=nao-e-data")).GET().build());
        assertThat(error.statusCode()).isEqualTo(400);
        Map<String, Object> errorJson = json.readValue(error.body(), Map.class);
        assertThat(errorJson.keySet()).containsExactlyInAnyOrder(
                "timestamp", "status", "error", "message", "path", "fieldErrors");
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
