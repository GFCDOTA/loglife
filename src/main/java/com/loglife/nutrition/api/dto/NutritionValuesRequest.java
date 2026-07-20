package com.loglife.nutrition.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * User-supplied nutrition values for one stored portion (label values on create, corrections on
 * edit). Partial values are not allowed: all four macros are required so totals stay coherent.
 */
public record NutritionValuesRequest(
        @NotNull(message = "nutrition.calories is required")
        @PositiveOrZero(message = "nutrition.calories must not be negative")
        BigDecimal calories,

        @NotNull(message = "nutrition.proteinGrams is required")
        @PositiveOrZero(message = "nutrition.proteinGrams must not be negative")
        BigDecimal proteinGrams,

        @NotNull(message = "nutrition.carbsGrams is required")
        @PositiveOrZero(message = "nutrition.carbsGrams must not be negative")
        BigDecimal carbsGrams,

        @NotNull(message = "nutrition.fatGrams is required")
        @PositiveOrZero(message = "nutrition.fatGrams must not be negative")
        BigDecimal fatGrams) {
}
