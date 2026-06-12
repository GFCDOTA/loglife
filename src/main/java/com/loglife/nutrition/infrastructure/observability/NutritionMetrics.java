package com.loglife.nutrition.infrastructure.observability;

import com.loglife.nutrition.domain.EstimationSource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Micrometer counters for the nutrition module. Exposed through Actuator
 * ({@code /actuator/metrics}, {@code /actuator/prometheus}).
 *
 * <ul>
 *   <li>{@code loglife.nutrition.estimates{source=...}} — estimates produced, by source
 *       (recorded for every provider, including the default mock).</li>
 *   <li>{@code loglife.nutrition.primary.failures{provider=...}} — failures of the primary
 *       local agent, tagged by the active provider (local-agent / ollama).</li>
 *   <li>{@code loglife.nutrition.estimation.fallbacks} — times the controlled fallback was used.</li>
 * </ul>
 */
@Component
public class NutritionMetrics {

    private final MeterRegistry registry;
    private final Counter fallbacks;
    private final Map<String, Counter> estimateCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> primaryFailureCounters = new ConcurrentHashMap<>();

    public NutritionMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.fallbacks = Counter.builder("loglife.nutrition.estimation.fallbacks")
                .description("Number of times the controlled fallback estimator was used")
                .register(registry);
    }

    /** One increment per produced estimate, tagged by its source. */
    public void recordEstimate(EstimationSource source) {
        estimateCounters.computeIfAbsent(source.name(), name ->
                Counter.builder("loglife.nutrition.estimates")
                        .tag("source", name)
                        .description("Number of nutrition estimates produced")
                        .register(registry)).increment();
    }

    /** One increment per primary-estimator failure, tagged by the active provider. */
    public void recordPrimaryFailure(String provider) {
        String tag = (provider == null || provider.isBlank()) ? "unknown" : provider;
        primaryFailureCounters.computeIfAbsent(tag, name ->
                Counter.builder("loglife.nutrition.primary.failures")
                        .tag("provider", name)
                        .description("Number of primary (local agent) estimation failures")
                        .register(registry)).increment();
    }

    public void recordFallback() {
        fallbacks.increment();
    }
}
