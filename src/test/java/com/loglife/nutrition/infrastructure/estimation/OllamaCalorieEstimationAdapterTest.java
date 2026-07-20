package com.loglife.nutrition.infrastructure.estimation;

import com.loglife.nutrition.application.port.out.EstimationResult;
import com.loglife.nutrition.domain.EstimatedItem;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodDescription;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionEstimate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Contract test for the Ollama estimator. Uses {@link MockRestServiceServer} to stand in for the
 * local Ollama server, pinning the request (model, stream=false, keep_alive, JSON-Schema format)
 * and the parsing of the chat response. The real model is not exercised here.
 */
class OllamaCalorieEstimationAdapterTest {

    private static final String BASE_URL = "http://localhost:11434";
    private static final String MODEL = "llama3.1:8b";
    private static final String KEEP_ALIVE = "30m";

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final FoodDescription DESCRIPTION = new FoodDescription(
            "2 bifes médios e 200g de arroz", LocalDate.of(2026, 6, 12), MealType.LUNCH,
            FoodQuantity.none(), "pt-BR");

    // A valid estimate JSON, WITHOUT an overall "confidence" so confidence must be item-derived.
    private static final String ESTIMATE_JSON = """
            {
              "items": [
                {"name":"bife bovino grelhado","quantity":2,"unit":"unidade média",
                 "calories":420,"proteinGrams":52,"carbsGrams":0,"fatGrams":22,"confidence":0.8},
                {"name":"arroz cozido","quantity":200,"unit":"g",
                 "calories":260,"proteinGrams":5,"carbsGrams":56,"fatGrams":1,"confidence":0.8}
              ],
              "explanation": "Estimado por porção a partir da descrição."
            }""";

    private static String chatResponseWrapping(String content) {
        // Ollama's /api/chat returns {"message":{"role":..,"content":"<the model's text>"}}.
        return "{\"message\":{\"role\":\"assistant\",\"content\":" + MAPPER.writeValueAsString(content) + "}}";
    }

    private OllamaCalorieEstimationAdapter adapter(RestClient.Builder builder) {
        return new OllamaCalorieEstimationAdapter(builder.build(), MODEL, KEEP_ALIVE, MAPPER);
    }

    @Test
    void sendsSchemaAndKeepAliveThenParsesItemDerivedConfidence() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/api/chat"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.model").value(MODEL))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.keep_alive").value(KEEP_ALIVE))
                .andExpect(jsonPath("$.format.type").value("object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andRespond(withSuccess(chatResponseWrapping(ESTIMATE_JSON), MediaType.APPLICATION_JSON));

        EstimationResult result = adapter(builder).estimate(DESCRIPTION);

        assertThat(result.isSuccess()).isTrue();
        NutritionEstimate estimate = result.estimate().orElseThrow();
        assertThat(estimate.source()).isEqualTo(EstimationSource.OLLAMA);
        assertThat(estimate.items()).extracting(EstimatedItem::name)
                .containsExactly("bife bovino grelhado", "arroz cozido");
        assertThat(estimate.nutrition().calories()).isEqualByComparingTo("680");
        // Confidence is the mean of the item confidences (0.8, 0.8), NOT a fixed 0.5 placeholder.
        assertThat(estimate.confidence().value()).isCloseTo(0.8, within(0.01));
        server.verify();
    }

    @Test
    void retriesOnceThenFailsOnUnparseableContent() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(ExpectedCount.times(2), requestTo(BASE_URL + "/api/chat"))
                .andExpect(method(POST))
                .andRespond(withSuccess(chatResponseWrapping("desculpe, não consegui"), MediaType.APPLICATION_JSON));

        EstimationResult result = adapter(builder).estimate(DESCRIPTION);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failureReason()).contains("attempts");
        server.verify(); // proves it retried exactly twice
    }

    @Test
    void failsWithoutRetryWhenMessageContentMissing() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(ExpectedCount.once(), requestTo(BASE_URL + "/api/chat"))
                .andRespond(withSuccess("{\"message\":{\"role\":\"assistant\"}}", MediaType.APPLICATION_JSON));

        EstimationResult result = adapter(builder).estimate(DESCRIPTION);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failureReason()).contains("no message content");
        server.verify();
    }
}
