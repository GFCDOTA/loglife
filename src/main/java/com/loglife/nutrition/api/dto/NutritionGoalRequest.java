package com.loglife.nutrition.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Request body for {@code PUT /api/v1/nutrition/goal}. Calories are mandatory; macro targets are
 * optional refinements (null = no target for that macro).
 */
public record NutritionGoalRequest(
        @NotNull(message = "calories is required")
        @Positive(message = "calories must be positive")
        BigDecimal calories,

        @PositiveOrZero(message = "proteinGrams must not be negative")
        BigDecimal proteinGrams,

        @PositiveOrZero(message = "carbsGrams must not be negative")
        BigDecimal carbsGrams,

        @PositiveOrZero(message = "fatGrams must not be negative")
        BigDecimal fatGrams) {
}
