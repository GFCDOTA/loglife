package com.loglife.nutrition.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * API representation of a {@code FoodLog}.
 */
public record FoodLogResponse(
        UUID id,
        LocalDate date,
        String mealType,
        String descriptionOriginal,
        String normalizedFoodName,
        BigDecimal quantity,
        String unit,
        BigDecimal calories,
        BigDecimal proteinGrams,
        BigDecimal carbsGrams,
        BigDecimal fatGrams,
        double confidence,
        String source,
        String notes,
        String explanation,
        Instant createdAt,
        Instant updatedAt) {
}
