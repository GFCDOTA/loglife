package com.loglife.nutrition.infrastructure.estimation;

import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.EstimationResult;
import com.loglife.nutrition.domain.FoodDescription;
import com.loglife.nutrition.infrastructure.observability.NutritionMetrics;

import java.util.Objects;

/**
 * Decorator that records the {@code loglife.nutrition.estimates{source=...}} metric for every
 * successful estimate, regardless of which provider produced it. Wrapping the outermost
 * estimator (mock-only or composite) guarantees the counter fires on every provider path —
 * including the default {@code mock}.
 */
public class MetricsRecordingCalorieEstimationAdapter implements CalorieEstimationPort {

    private final CalorieEstimationPort delegate;
    private final NutritionMetrics metrics;

    public MetricsRecordingCalorieEstimationAdapter(CalorieEstimationPort delegate, NutritionMetrics metrics) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    @Override
    public EstimationResult estimate(FoodDescription description) {
        EstimationResult result = delegate.estimate(description);
        if (result != null && result.isSuccess()) {
            result.estimate().ifPresent(estimate -> metrics.recordEstimate(estimate.source()));
        }
        return result;
    }
}
