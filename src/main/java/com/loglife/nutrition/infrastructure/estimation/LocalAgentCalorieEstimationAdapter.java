package com.loglife.nutrition.infrastructure.estimation;

import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.EstimationResult;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodDescription;
import com.loglife.nutrition.domain.NutritionEstimate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/**
 * Calls a custom local HTTP agent that implements the {@code POST /estimate-calories} contract.
 * The {@link RestClient} is pre-configured (base URL + timeouts) by
 * {@link com.loglife.nutrition.infrastructure.config.EstimationConfiguration}.
 *
 * <p>This adapter never throws for transport/agent failures; it returns
 * {@link EstimationResult#failure} so the composite can fall back.
 */
public class LocalAgentCalorieEstimationAdapter implements CalorieEstimationPort {

    private static final Logger log = LoggerFactory.getLogger(LocalAgentCalorieEstimationAdapter.class);

    private final RestClient restClient;

    public LocalAgentCalorieEstimationAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public EstimationResult estimate(FoodDescription description) {
        try {
            AgentDtos.EstimateRequest request = new AgentDtos.EstimateRequest(
                    description.rawText(), description.language(), description.date().toString());

            AgentDtos.EstimateResponse response = restClient.post()
                    .uri("/estimate-calories")
                    .body(request)
                    .retrieve()
                    .body(AgentDtos.EstimateResponse.class);

            NutritionEstimate estimate = EstimateResponseMapper.toDomain(response, EstimationSource.LOCAL_AGENT);
            if (estimate == null) {
                return EstimationResult.failure("local agent returned an empty estimate");
            }
            log.debug("Local agent produced estimate: items={} calories={}",
                    estimate.items().size(), estimate.nutrition().calories());
            return EstimationResult.success(estimate);
        } catch (Exception ex) {
            log.warn("Local agent estimation failed: {}", ex.toString());
            return EstimationResult.failure("local agent error: " + ex.getMessage());
        }
    }
}
