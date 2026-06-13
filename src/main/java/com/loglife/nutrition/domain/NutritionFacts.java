package com.loglife.nutrition.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object holding the nutritional totals for a portion of food. All values are
 * non-negative; a {@code null} component is normalised to {@link BigDecimal#ZERO}.
 *
 * <p>Every component is normalised to scale 2 (two decimal places, {@link RoundingMode#HALF_UP}),
 * matching the {@code NUMERIC(10,2)} storage columns, so the value held in memory is exactly the
 * value that is persisted — daily totals computed before saving stay equal after reloading.
 */
public record NutritionFacts(
        BigDecimal calories,
        BigDecimal proteinGrams,
        BigDecimal carbsGrams,
        BigDecimal fatGrams) {

    public NutritionFacts {
        calories = requireNonNegative(calories, "calories");
        proteinGrams = requireNonNegative(proteinGrams, "proteinGrams");
        carbsGrams = requireNonNegative(carbsGrams, "carbsGrams");
        fatGrams = requireNonNegative(fatGrams, "fatGrams");
    }

    public static NutritionFacts zero() {
        return new NutritionFacts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** Component-wise sum, used to aggregate daily totals. */
    public NutritionFacts plus(NutritionFacts other) {
        Objects.requireNonNull(other, "other");
        return new NutritionFacts(
                calories.add(other.calories),
                proteinGrams.add(other.proteinGrams),
                carbsGrams.add(other.carbsGrams),
                fatGrams.add(other.fatGrams));
    }

    private static BigDecimal requireNonNegative(BigDecimal candidate, String field) {
        BigDecimal value = candidate == null ? BigDecimal.ZERO : candidate;
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative, got: " + value);
        }
        // Normalise to the persisted precision so in-memory totals match what the DB stores.
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
