package com.loglife.nutrition.infrastructure.config;

import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.infrastructure.estimation.CompositeCalorieEstimationAdapter;
import com.loglife.nutrition.infrastructure.estimation.EstimationProperties;
import com.loglife.nutrition.infrastructure.estimation.LocalAgentCalorieEstimationAdapter;
import com.loglife.nutrition.infrastructure.estimation.MetricsRecordingCalorieEstimationAdapter;
import com.loglife.nutrition.infrastructure.estimation.MockCalorieEstimationAdapter;
import com.loglife.nutrition.infrastructure.estimation.OllamaCalorieEstimationAdapter;
import com.loglife.nutrition.infrastructure.observability.NutritionMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
 *
 * <p>{@code loglife.nutrition.estimation.fallback-to-mock=false} removes the mock fallback, so a
 * failing primary surfaces as 503 instead of silently logging mock numbers.
 */
@Configuration
public class EstimationConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EstimationConfiguration.class);

    @Bean
    MockCalorieEstimationAdapter mockCalorieEstimationAdapter() {
        return new MockCalorieEstimationAdapter();
    }

    @Bean
    @Primary
    CalorieEstimationPort calorieEstimationPort(EstimationProperties properties,
                                                MockCalorieEstimationAdapter mock,
                                                NutritionMetrics metrics,
                                                ObjectMapper objectMapper) {
        String provider = properties.getProvider() == null
                ? "mock"
                : properties.getProvider().trim().toLowerCase();

        CalorieEstimationPort selected = switch (provider) {
            case "local-agent" -> {
                log.info("Calorie estimation provider: local-agent ({})",
                        properties.getLocalAgent().getBaseUrl());
                RestClient client = restClient(
                        properties.getLocalAgent().getBaseUrl(),
                        properties.getLocalAgent().getTimeout(),
                        properties.getLocalAgent().getTimeout());
                CalorieEstimationPort primary = new LocalAgentCalorieEstimationAdapter(client);
                yield withOptionalFallback(primary, properties, mock, metrics, "local-agent");
            }
            case "ollama" -> {
                log.info("Calorie estimation provider: ollama (model={} at {})",
                        properties.getOllama().getModel(), properties.getOllama().getBaseUrl());
                // Short connect (fail over to mock fast if Ollama is down) but long read
                // (a cold model load can take tens of seconds).
                RestClient client = restClient(
                        properties.getOllama().getBaseUrl(),
                        properties.getOllama().getConnectTimeout(),
                        properties.getOllama().getTimeout());
                CalorieEstimationPort primary = new OllamaCalorieEstimationAdapter(
                        client, properties.getOllama().getModel(),
                        properties.getOllama().getKeepAlive(), objectMapper);
                yield withOptionalFallback(primary, properties, mock, metrics, "ollama");
            }
            default -> {
                log.info("Calorie estimation provider: mock (no local agent configured)");
                yield mock;
            }
        };

        // Record the estimate-count metric uniformly for every provider (incl. mock).
        return new MetricsRecordingCalorieEstimationAdapter(selected, metrics);
    }

    private static CalorieEstimationPort withOptionalFallback(CalorieEstimationPort primary,
                                                              EstimationProperties properties,
                                                              MockCalorieEstimationAdapter mock,
                                                              NutritionMetrics metrics,
                                                              String providerName) {
        if (properties.isFallbackToMock()) {
            return new CompositeCalorieEstimationAdapter(primary, mock, metrics, providerName);
        }
        log.info("Mock fallback disabled: a failing '{}' estimator surfaces as 503", providerName);
        return primary;
    }

    private static RestClient restClient(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout.toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
