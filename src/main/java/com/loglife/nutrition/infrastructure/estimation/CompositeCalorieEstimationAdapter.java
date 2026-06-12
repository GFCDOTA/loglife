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
 * estimator (the mock). Records metrics for failures and fallbacks, and logs when the fallback
 * is triggered. Both branches are defensive — a primary that throws despite the port contract
 * is treated as a failure, not a crash.
 */
public class CompositeCalorieEstimationAdapter implements CalorieEstimationPort {

    private static final Logger log = LoggerFactory.getLogger(CompositeCalorieEstimationAdapter.class);

    private final CalorieEstimationPort primary;
    private final CalorieEstimationPort fallback;
    private final NutritionMetrics metrics;

    public CompositeCalorieEstimationAdapter(CalorieEstimationPort primary,
                                             CalorieEstimationPort fallback,
                                             NutritionMetrics metrics) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    @Override
    public EstimationResult estimate(FoodDescription description) {
        EstimationResult primaryResult = safeEstimate(primary, description);
        if (primaryResult.isSuccess()) {
            primaryResult.estimate().ifPresent(e -> metrics.recordEstimate(e.source()));
            return primaryResult;
        }

        log.warn("Primary estimator failed ({}), using controlled fallback", primaryResult.failureReason());
        metrics.recordLocalAgentFailure();

        EstimationResult fallbackResult = safeEstimate(fallback, description);
        if (fallbackResult.isSuccess()) {
            metrics.recordFallback();
            fallbackResult.estimate().ifPresent(e -> metrics.recordEstimate(e.source()));
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
