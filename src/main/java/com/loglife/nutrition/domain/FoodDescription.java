package com.loglife.nutrition.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The user's free-text description of what they ate, plus light context. This is the input
 * to a {@code CalorieEstimationPort}. Kept free of any framework or transport concern.
 */
public record FoodDescription(
        String rawText,
        LocalDate date,
        MealType mealType,
        FoodQuantity quantity,
        String language) {

    public FoodDescription {
        if (rawText == null || rawText.trim().length() < 2) {
            throw new IllegalArgumentException("description must have at least 2 characters");
        }
        rawText = rawText.trim();
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(mealType, "mealType");
        if (quantity == null) {
            quantity = FoodQuantity.none();
        }
        if (language == null || language.isBlank()) {
            language = "pt-BR";
        }
    }
}
