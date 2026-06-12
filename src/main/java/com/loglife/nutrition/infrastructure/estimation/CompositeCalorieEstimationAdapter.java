package com.loglife.nutrition.infrastructure.estimation;

import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.EstimationResult;
import com.loglife.nutrition.domain.FoodDescription;
import com.loglife.nutrition.infrastructure.observability.NutritionMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Tries a primary estimator (a local agent) first; on failure, falls back to a controlled
 * estimator (the mock). Records the primary-failure and fallback metrics (the estimate-count
 * metric is recorded uniformly by {@link MetricsRecordingCalorieEstimationAdapter}, which wraps
 * this one). Both branches are defensive — a primary that throws despite the port contract is
 * treated as a failure, not a crash.
 */
public class CompositeCalorieEstimationAdapter implements CalorieEstimationPort {

    private static final Logger log = LoggerFactory.getLogger(CompositeCalorieEstimationAdapter.class);

    private final CalorieEstimationPort primary;
    private final CalorieEstimationPort fallback;
    private final NutritionMetrics metrics;
    private final String providerName;

    public CompositeCalorieEstimationAdapter(CalorieEstimationPort primary,
                                             CalorieEstimationPort fallback,
                                             NutritionMetrics metrics,
                                             String providerName) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.providerName = (providerName == null || providerName.isBlank()) ? "unknown" : providerName;
    }

    @Override
    public EstimationResult estimate(FoodDescription description) {
        EstimationResult primaryResult = safeEstimate(primary, description);
        if (primaryResult.isSuccess()) {
            return primaryResult;
        }

        log.warn("Primary estimator '{}' failed ({}), using controlled fallback",
                providerName, primaryResult.failureReason());
        metrics.recordPrimaryFailure(providerName);

        EstimationResult fallbackResult = safeEstimate(fallback, description);
        if (fallbackResult.isSuccess()) {
            metrics.recordFallback();
        }
        return fallbackResult;
    }

    private static EstimationResult safeEstimate(CalorieEstimationPort port, FoodDescription description) {
        try {
            EstimationResult result = port.estimate(description);
            return result != null ? result : EstimationResult.failure("estimator returned null");
        } catch (RuntimeException ex) {
            return EstimationResult.failure("estimator threw: " + ex.getMessage());
        }
    }
}
