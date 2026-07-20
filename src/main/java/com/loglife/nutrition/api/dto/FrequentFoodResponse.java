package com.loglife.nutrition.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One frequently-logged food. {@code logId} is the latest entry of this food — POST
 * {@code /api/v1/food-logs/{logId}/repeat} clones it onto a new date without any estimation.
 */
public record FrequentFoodResponse(
        UUID logId,
        String name,
        int timesLogged,
        String mealType,
        BigDecimal calories,
        BigDecimal proteinGrams,
        BigDecimal carbsGrams,
        BigDecimal fatGrams,
        BigDecimal quantity,
        String unit,
        String source) {
}
