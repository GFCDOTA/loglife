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
 *   <li>{@code loglife.nutrition.estimates{source=...}} — estimates produced, by source.</li>
 *   <li>{@code loglife.nutrition.local_agent.failures} — primary (local agent) failures.</li>
 *   <li>{@code loglife.nutrition.estimation.fallbacks} — times the fallback estimator was used.</li>
 * </ul>
 */
@Component
public class NutritionMetrics {

    private final MeterRegistry registry;
    private final Counter localAgentFailures;
    private final Counter fallbacks;
    private final Map<String, Counter> estimateCounters = new ConcurrentHashMap<>();

    public NutritionMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.localAgentFailures = Counter.builder("loglife.nutrition.local_agent.failures")
                .description("Number of primary local-agent estimation failures")
                .register(registry);
        this.fallbacks = Counter.builder("loglife.nutrition.estimation.fallbacks")
                .description("Number of times the controlled fallback estimator was used")
                .register(registry);
    }

    public void recordEstimate(EstimationSource source) {
        estimateCounters.computeIfAbsent(source.name(), name ->
                Counter.builder("loglife.nutrition.estimates")
                        .tag("source", name)
                        .description("Number of nutrition estimates produced")
                        .register(registry)).increment();
    }

    public void recordLocalAgentFailure() {
        localAgentFailures.increment();
    }

    public void recordFallback() {
        fallbacks.increment();
    }
}
