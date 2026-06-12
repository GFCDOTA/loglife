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
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

/**
 * Contract-style test for the custom local agent ({@code POST /estimate-calories}). It pins the
 * exact request/response shape from the agreed contract so a drift on either side is caught.
 */
class LocalAgentCalorieEstimationAdapterTest {

    // The exact sample response from the local-agent contract.
    private static final String SAMPLE_RESPONSE = """
            {
              "items": [
                {
                  "name": "bife bovino grelhado",
                  "quantity": 2,
                  "unit": "unidade média",
                  "calories": 420,
                  "proteinGrams": 52,
                  "carbsGrams": 0,
                  "fatGrams": 22,
                  "confidence": 0.72
                },
                {
                  "name": "arroz cozido",
                  "quantity": 200,
                  "unit": "g",
                  "calories": 260,
                  "proteinGrams": 5,
                  "carbsGrams": 56,
                  "fatGrams": 1,
                  "confidence": 0.85
                }
              ],
              "total": {
                "calories": 680,
                "proteinGrams": 57,
                "carbsGrams": 56,
                "fatGrams": 23
              },
              "source": "LOCAL_AGENT",
              "confidence": 0.78,
              "explanation": "Estimativa baseada nos alimentos informados pelo usuário."
            }
            """;

    private static final FoodDescription DESCRIPTION = new FoodDescription(
            "2 bifes médios e 200g de arroz", LocalDate.of(2026, 6, 12), MealType.LUNCH,
            FoodQuantity.none(), "pt-BR");

    @Test
    void parsesContractResponseIntoDomainEstimate() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8787");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8787/estimate-calories"))
                .andExpect(method(POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.description").value("2 bifes médios e 200g de arroz"))
                .andExpect(jsonPath("$.language").value("pt-BR"))
                .andExpect(jsonPath("$.date").value("2026-06-12"))
                .andRespond(withSuccess(SAMPLE_RESPONSE, MediaType.APPLICATION_JSON));

        LocalAgentCalorieEstimationAdapter adapter =
                new LocalAgentCalorieEstimationAdapter(builder.build());

        EstimationResult result = adapter.estimate(DESCRIPTION);

        assertThat(result.isSuccess()).isTrue();
        NutritionEstimate estimate = result.estimate().orElseThrow();
        assertThat(estimate.source()).isEqualTo(EstimationSource.LOCAL_AGENT);
        assertThat(estimate.confidence().value()).isEqualTo(0.78);
        assertThat(estimate.nutrition().calories()).isEqualByComparingTo("680");
        assertThat(estimate.nutrition().proteinGrams()).isEqualByComparingTo("57");
        assertThat(estimate.nutrition().carbsGrams()).isEqualByComparingTo("56");
        assertThat(estimate.nutrition().fatGrams()).isEqualByComparingTo("23");
        assertThat(estimate.items()).hasSize(2);
        assertThat(estimate.items()).extracting(EstimatedItem::name)
                .containsExactly("bife bovino grelhado", "arroz cozido");
        server.verify();
    }

    @Test
    void returnsFailureWhenAgentErrors() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8787");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8787/estimate-calories"))
                .andRespond(withServerError());

        LocalAgentCalorieEstimationAdapter adapter =
                new LocalAgentCalorieEstimationAdapter(builder.build());

        EstimationResult result = adapter.estimate(DESCRIPTION);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failureReason()).contains("local agent");
    }
}
