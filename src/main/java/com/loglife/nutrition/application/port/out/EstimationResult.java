package com.loglife.nutrition.application.port.out;

import com.loglife.nutrition.domain.NutritionEstimate;

import java.util.Objects;
import java.util.Optional;

/**
 * Explicit result type for a calorie estimation attempt: either a successful
 * {@link NutritionEstimate} or a failure carrying a human-readable reason. Using a Result type
 * (instead of throwing) keeps the expected "the local agent is down" path out of exception
 * control flow, so {@code CompositeCalorieEstimationAdapter} can fall back deterministically.
 */
public final class EstimationResult {

    private final NutritionEstimate estimate;
    private final String failureReason;

    private EstimationResult(NutritionEstimate estimate, String failureReason) {
        this.estimate = estimate;
        this.failureReason = failureReason;
    }

    public static EstimationResult success(NutritionEstimate estimate) {
        return new EstimationResult(Objects.requireNonNull(estimate, "estimate"), null);
    }

    public static EstimationResult failure(String reason) {
        return new EstimationResult(null, (reason == null || reason.isBlank()) ? "unknown" : reason);
    }

    public boolean isSuccess() {
        return estimate != null;
    }

    public boolean isFailure() {
        return estimate == null;
    }

    public Optional<NutritionEstimate> estimate() {
        return Optional.ofNullable(estimate);
    }

    public String failureReason() {
        return failureReason;
    }
}
