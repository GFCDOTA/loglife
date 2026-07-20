package com.loglife.nutrition.api.dto;

import java.math.BigDecimal;

/**
 * API representation of the user's daily nutrition goal.
 */
public record NutritionGoalResponse(
        BigDecimal calories,
        BigDecimal proteinGrams,
        BigDecimal carbsGrams,
        BigDecimal fatGrams) {
}
