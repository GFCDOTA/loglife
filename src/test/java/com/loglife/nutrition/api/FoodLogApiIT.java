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
    void manualNutritionEntryBypassesEstimation() throws Exception {
        String body = """
                {
                  "date": "%s",
                  "mealType": "SNACK",
                  "description": "barra de proteína (rótulo)",
                  "quantity": 45,
                  "unit": "g",
                  "nutrition": { "calories": 180, "proteinGrams": 20, "carbsGrams": 15, "fatGrams": 5 }
                }
                """.formatted(DATE);
        HttpResponse<String> created = send(request("/api/v1/food-logs")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build());

        assertThat(created.statusCode()).isEqualTo(201);
        List<Object> createdJson = json.readValue(created.body(), List.class);
        assertThat(createdJson).hasSize(1);
        Map<String, Object> log = (Map<String, Object>) createdJson.get(0);
        assertThat(log.get("source")).isEqualTo("MANUAL");
        assertThat(((Number) log.get("confidence")).doubleValue()).isEqualTo(1.0);
        assertThat(((Number) log.get("calories")).doubleValue()).isEqualTo(180.0);
        assertThat(((Number) log.get("quantity")).doubleValue()).isEqualTo(45.0);
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
    void frequentFoodsFeedOneTapRepeatWithoutEstimation() throws Exception {
        // Log the same snack twice (manual values -> deterministic, no estimator noise).
        for (int i = 0; i < 2; i++) {
            HttpResponse<String> created = send(request("/api/v1/food-logs")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {
                              "date": "%s", "mealType": "SNACK", "description": "iogurte com granola",
                              "nutrition": { "calories": 220, "proteinGrams": 12, "carbsGrams": 30, "fatGrams": 6 }
                            }
                            """.formatted(DATE), StandardCharsets.UTF_8)).build());
            assertThat(created.statusCode()).isEqualTo(201);
        }

        // frequent: grouped to ONE chip with count 2
        HttpResponse<String> frequent =
                send(request("/api/v1/food-logs/frequent?date=" + DATE).GET().build());
        assertThat(frequent.statusCode()).isEqualTo(200);
        List<Object> chips = json.readValue(frequent.body(), List.class);
        assertThat(chips).hasSize(1);
        Map<String, Object> chip = (Map<String, Object>) chips.get(0);
        assertThat(chip.get("name")).isEqualTo("iogurte com granola");
        assertThat(((Number) chip.get("timesLogged")).intValue()).isEqualTo(2);
        String sourceLogId = (String) chip.get("logId");

        // repeat onto another day: clones nutrition, keeps provenance, no LLM involved
        HttpResponse<String> repeated = send(request("/api/v1/food-logs/" + sourceLogId + "/repeat")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        { "date": "2026-06-13" }
                        """, StandardCharsets.UTF_8)).build());
        assertThat(repeated.statusCode()).isEqualTo(201);
        Map<String, Object> repeatedLog = json.readValue(repeated.body(), Map.class);
        assertThat(repeatedLog.get("date")).isEqualTo("2026-06-13");
        assertThat(((Number) repeatedLog.get("calories")).doubleValue()).isEqualTo(220.0);
        assertThat(repeatedLog.get("source")).isEqualTo("MANUAL");
        assertThat(repeatedLog.get("id")).isNotEqualTo(sourceLogId);

        // the new day now lists exactly the repeated entry
        HttpResponse<String> nextDay = send(request("/api/v1/food-logs?date=2026-06-13").GET().build());
        assertThat(json.readValue(nextDay.body(), List.class)).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingDateDefaultsToTodayInConfiguredTimezone() throws Exception {
        String body = """
                {
                  "mealType": "SNACK", "description": "iogurte por atalho de voz",
                  "nutrition": { "calories": 100, "proteinGrams": 10, "carbsGrams": 10, "fatGrams": 1 }
                }
                """;
        HttpResponse<String> created = send(request("/api/v1/food-logs")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build());

        assertThat(created.statusCode()).isEqualTo(201);
        Map<String, Object> log = (Map<String, Object>) json.readValue(created.body(), List.class).get(0);
        String expected = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).toString();
        assertThat(log.get("date")).isEqualTo(expected);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reestimatesLogInPlaceKeepingIdentity() throws Exception {
        HttpResponse<String> created = send(request("/api/v1/food-logs")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        { "date": "%s", "mealType": "DINNER", "description": "sopa de legumes" }
                        """.formatted(DATE), StandardCharsets.UTF_8)).build());
        Matcher matcher = ID_PATTERN.matcher(created.body());
        assertThat(matcher.find()).isTrue();
        String id = matcher.group(1);

        HttpResponse<String> reestimated = send(request("/api/v1/food-logs/" + id + "/re-estimate")
                .POST(HttpRequest.BodyPublishers.noBody()).build());

        assertThat(reestimated.statusCode()).isEqualTo(200);
        Map<String, Object> log = json.readValue(reestimated.body(), Map.class);
        assertThat(log.get("id")).isEqualTo(id);
        assertThat(log.get("date")).isEqualTo(DATE);
        assertThat(((Number) log.get("calories")).doubleValue()).isGreaterThan(0.0);

        // still ONE row for the day
        HttpResponse<String> list = send(request("/api/v1/food-logs?date=" + DATE).GET().build());
        assertThat(json.readValue(list.body(), List.class)).hasSize(1);

        // unknown id -> structured 404
        HttpResponse<String> missing = send(request("/api/v1/food-logs/" + UUID.randomUUID() + "/re-estimate")
                .POST(HttpRequest.BodyPublishers.noBody()).build());
        assertThat(missing.statusCode()).isEqualTo(404);
    }

    @Test
    void exportsCsvForThePeriod() throws Exception {
        send(request("/api/v1/food-logs")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "date": "%s", "mealType": "LUNCH", "description": "arroz, feijão e \\"bife\\"",
                          "nutrition": { "calories": 600, "proteinGrams": 40, "carbsGrams": 60, "fatGrams": 15 }
                        }
                        """.formatted(DATE), StandardCharsets.UTF_8)).build());

        HttpResponse<String> csv = send(
                request("/api/v1/food-logs/export?from=2026-06-01&to=2026-06-30").GET().build());

        assertThat(csv.statusCode()).isEqualTo(200);
        assertThat(csv.headers().firstValue("Content-Type").orElse("")).startsWith("text/csv");
        assertThat(csv.headers().firstValue("Content-Disposition").orElse(""))
                .contains("loglife_2026-06-01_2026-06-30.csv");
        List<String> lines = csv.body().lines().toList();
        assertThat(lines.get(0)).startsWith("date,mealType,name");
        assertThat(lines).hasSize(2);
        assertThat(lines.get(1)).contains("\"arroz, feijão e \"\"bife\"\"\"");

        // inverted period is the client's mistake -> structured 400, not a 500
        HttpResponse<String> inverted = send(
                request("/api/v1/food-logs/export?from=2026-06-30&to=2026-06-01").GET().build());
        assertThat(inverted.statusCode()).isEqualTo(400);
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
