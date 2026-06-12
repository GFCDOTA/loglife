package com.loglife.nutrition.domain;

import java.math.BigDecimal;

/**
 * A single food item within a {@link NutritionEstimate}, e.g. "arroz cozido, 200 g".
 */
public record EstimatedItem(
        String name,
        BigDecimal quantity,
        String unit,
        NutritionFacts nutrition,
        Confidence confidence) {

    public EstimatedItem {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("estimated item name must not be blank");
        }
        name = name.trim();
        if (nutrition == null) {
            nutrition = NutritionFacts.zero();
        }
        if (confidence == null) {
            confidence = Confidence.ZERO;
        }
    }
}
