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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full black-box API test: real HTTP against the running server (random port), real PostgreSQL
 * (Testcontainers), default {@code mock} estimation provider (no external agent needed).
 */
class FoodLogApiIT extends AbstractPostgresIntegrationTest {

    private static final String DATE = "2026-06-12";
    private static final Pattern ID_PATTERN =
            Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @Value("${local.server.port}")
    private int port;

    private String base() {
        return "http://localhost:" + port;
    }

    @Test
    @SuppressWarnings("unchecked")
    void createListSummarizeAndDeleteFoodLog() throws Exception {
        // 1. create
        String createBody = """
                {
                  "date": "%s",
                  "mealType": "LUNCH",
                  "description": "2 bifes médios e 200g de arroz",
                  "notes": "almoço"
                }
                """.formatted(DATE);
        HttpResponse<String> created = send(request("/api/v1/food-logs")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createBody, StandardCharsets.UTF_8)).build());

        assertThat(created.statusCode()).isEqualTo(201);
        // POST now returns an array: one log per food item (MOCK yields a single item).
        List<Object> createdJson = json.readValue(created.body(), List.class);
        assertThat(createdJson).hasSize(1);
        Map<String, Object> first = (Map<String, Object>) createdJson.get(0);
        assertThat(first.get("mealType")).isEqualTo("LUNCH");
        assertThat(first.get("source")).isEqualTo("MOCK");
        assertThat(((Number) first.get("calories")).doubleValue()).isGreaterThan(0.0);
        assertThat(((Number) first.get("confidence")).doubleValue()).isEqualTo(0.2);

        Matcher matcher = ID_PATTERN.matcher(created.body());
        assertThat(matcher.find()).isTrue();
        String id = matcher.group(1);

        // 2. list by date
        HttpResponse<String> list = send(request("/api/v1/food-logs?date=" + DATE).GET().build());
        assertThat(list.statusCode()).isEqualTo(200);
        List<Object> listJson = json.readValue(list.body(), List.class);
        assertThat(listJson).hasSize(1);

        // 3. daily summary
        HttpResponse<String> summary =
                send(request("/api/v1/nutrition/daily-summary?date=" + DATE).GET().build());
        assertThat(summary.statusCode()).isEqualTo(200);
        Map<String, Object> summaryJson = json.readValue(summary.body(), Map.class);
        assertThat(summaryJson.get("date")).isEqualTo(DATE);
        assertThat(((Number) summaryJson.get("totalLogs")).intValue()).isEqualTo(1);
        assertThat(((Number) summaryJson.get("totalCalories")).doubleValue()).isGreaterThan(0.0);
        Map<String, Object> byMeal = (Map<String, Object>) summaryJson.get("logsByMealType");
        assertThat(byMeal).containsKey("LUNCH");

        // 4. delete
        HttpResponse<String> deleted = send(request("/api/v1/food-logs/" + id).DELETE().build());
        assertThat(deleted.statusCode()).isEqualTo(204);

        // 5. gone
        HttpResponse<String> afterDelete = send(request("/api/v1/food-logs?date=" + DATE).GET().build());
        assertThat(json.readValue(afterDelete.body(), List.class)).isEmpty();

        // 6. deleting again -> 404 with structured error
        HttpResponse<String> deleteAgain = send(request("/api/v1/food-logs/" + id).DELETE().build());
        assertThat(deleteAgain.statusCode()).isEqualTo(404);
        Map<String, Object> error = json.readValue(deleteAgain.body(), Map.class);
        assertThat(((Number) error.get("status")).intValue()).isEqualTo(404);
    }

    @Test
    @SuppressWarnings("unchecked")
    void editsLogWithoutReestimatingAndSummaryReflectsOverride() throws Exception {
        // create (mock estimator decides the numbers)
        String createBody = """
                { "date": "%s", "mealType": "BREAKFAST", "description": "pão com manteiga" }
                """.formatted(DATE);
        HttpResponse<String> created = send(request("/api/v1/food-logs")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createBody, StandardCharsets.UTF_8)).build());
        assertThat(created.statusCode()).isEqualTo(201);
        Matcher matcher = ID_PATTERN.matcher(created.body());
        assertThat(matcher.find()).isTrue();
        String id = matcher.group(1);

        // the user corrects the numbers from the label — NO re-estimation
        String patchBody = """
                {
                  "notes": "valores do rótulo",
                  "nutrition": { "calories": 512, "proteinGrams": 9, "carbsGrams": 58, "fatGrams": 26 }
                }
                """;
        HttpResponse<String> patched = send(request("/api/v1/food-logs/" + id)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(patchBody, StandardCharsets.UTF_8))
                .build());

        assertThat(patched.statusCode()).isEqualTo(200);
        Map<String, Object> log = json.readValue(patched.body(), Map.class);
        assertThat(((Number) log.get("calories")).doubleValue()).isEqualTo(512.0);
        assertThat(log.get("source")).isEqualTo("USER_OVERRIDE");
        assertThat(((Number) log.get("confidence")).doubleValue()).isEqualTo(1.0);
        assertThat(log.get("notes")).isEqualTo("valores do rótulo");
        assertThat(log.get("mealType")).isEqualTo("BREAKFAST");

        // the day's totals use the corrected values
        HttpResponse<String> summary =
                send(request("/api/v1/nutrition/daily-summary?date=" + DATE).GET().build());
        Map<String, Object> summaryJson = json.readValue(summary.body(), Map.class);
        assertThat(((Number) summaryJson.get("totalCalories")).doubleValue()).isEqualTo(512.0);

        // unknown id -> structured 404
        HttpResponse<String> missing = send(request("/api/v1/food-logs/" + UUID.randomUUID())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(patchBody, StandardCharsets.UTF_8))
                .build());
        assertThat(missing.statusCode()).isEqualTo(404);
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectsInvalidRequestWithStructuredError() throws Exception {
        String body = """
                { "date": "%s", "mealType": "", "description": "x" }
                """.formatted(DATE);
        HttpResponse<String> response = send(request("/api/v1/food-logs")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build());

        assertThat(response.statusCode()).isEqualTo(400);
        Map<String, Object> error = json.readValue(response.body(), Map.class);
        assertThat(((Number) error.get("status")).intValue()).isEqualTo(400);
        assertThat((List<Object>) error.get("fieldErrors")).isNotEmpty();
    }

    @Test
    void rejectsUnknownMealType() throws Exception {
        String body = """
                { "date": "%s", "mealType": "BRUNCH", "description": "pão com ovo" }
                """.formatted(DATE);
        HttpResponse<String> response = send(request("/api/v1/food-logs")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build());

        assertThat(response.statusCode()).isEqualTo(400);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(base() + path));
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
