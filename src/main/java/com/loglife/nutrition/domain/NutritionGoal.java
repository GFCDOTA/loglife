package com.loglife.nutrition.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The user's daily nutrition target (single-user app: there is exactly one). Calories are
 * mandatory — the goal exists to answer "how much is left today". Macro targets are optional
 * refinements; {@code null} means "no target for this macro".
 */
public record NutritionGoal(
        BigDecimal calories,
        BigDecimal proteinGrams,
        BigDecimal carbsGrams,
        BigDecimal fatGrams) {

    public NutritionGoal {
        Objects.requireNonNull(calories, "calories");
        if (calories.signum() <= 0) {
            throw new IllegalArgumentException("goal calories must be positive");
        }
        requireNonNegative(proteinGrams, "proteinGrams");
        requireNonNegative(carbsGrams, "carbsGrams");
        requireNonNegative(fatGrams, "fatGrams");
    }

    private static void requireNonNegative(BigDecimal value, String name) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException("goal " + name + " must not be negative");
        }
    }
}
