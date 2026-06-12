package com.loglife.nutrition.infrastructure.config;

import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.infrastructure.estimation.CompositeCalorieEstimationAdapter;
import com.loglife.nutrition.infrastructure.estimation.EstimationProperties;
import com.loglife.nutrition.infrastructure.estimation.LocalAgentCalorieEstimationAdapter;
import com.loglife.nutrition.infrastructure.estimation.MockCalorieEstimationAdapter;
import com.loglife.nutrition.infrastructure.estimation.OllamaCalorieEstimationAdapter;
import com.loglife.nutrition.infrastructure.observability.NutritionMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Selects and wires the calorie-estimation strategy based on
 * {@code loglife.nutrition.estimation.provider}:
 * <ul>
 *   <li>{@code mock} — the controlled mock only (default; zero external dependencies).</li>
 *   <li>{@code local-agent} — the custom HTTP agent, wrapped with the mock fallback.</li>
 *   <li>{@code ollama} — a local Ollama LLM, wrapped with the mock fallback.</li>
 * </ul>
 */
@Configuration
public class EstimationConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EstimationConfiguration.class);

    @Bean
    MockCalorieEstimationAdapter mockCalorieEstimationAdapter() {
        return new MockCalorieEstimationAdapter();
    }

    @Bean
    CalorieEstimationPort calorieEstimationPort(EstimationProperties properties,
                                                MockCalorieEstimationAdapter mock,
                                                NutritionMetrics metrics,
                                                ObjectMapper objectMapper,
                                                RestClient.Builder restClientBuilder) {
        String provider = properties.getProvider() == null
                ? "mock"
                : properties.getProvider().trim().toLowerCase();

        return switch (provider) {
            case "local-agent" -> {
                log.info("Calorie estimation provider: local-agent ({})",
                        properties.getLocalAgent().getBaseUrl());
                RestClient client = restClient(restClientBuilder,
                        properties.getLocalAgent().getBaseUrl(),
                        properties.getLocalAgent().getTimeout());
                CalorieEstimationPort primary = new LocalAgentCalorieEstimationAdapter(client);
                yield new CompositeCalorieEstimationAdapter(primary, mock, metrics);
            }
            case "ollama" -> {
                log.info("Calorie estimation provider: ollama (model={} at {})",
                        properties.getOllama().getModel(), properties.getOllama().getBaseUrl());
                RestClient client = restClient(restClientBuilder,
                        properties.getOllama().getBaseUrl(),
                        properties.getOllama().getTimeout());
                CalorieEstimationPort primary =
                        new OllamaCalorieEstimationAdapter(client, properties.getOllama().getModel(), objectMapper);
                yield new CompositeCalorieEstimationAdapter(primary, mock, metrics);
            }
            default -> {
                log.info("Calorie estimation provider: mock (no local agent configured)");
                yield mock;
            }
        };
    }

    private static RestClient restClient(RestClient.Builder builder, String baseUrl, Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) timeout.toMillis());
        factory.setReadTimeout((int) timeout.toMillis());
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
