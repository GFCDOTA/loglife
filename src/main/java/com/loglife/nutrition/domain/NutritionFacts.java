package com.loglife.nutrition.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object holding the nutritional totals for a portion of food. All values are
 * non-negative; a {@code null} component is normalised to {@link BigDecimal#ZERO}.
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
        return value;
    }
}
