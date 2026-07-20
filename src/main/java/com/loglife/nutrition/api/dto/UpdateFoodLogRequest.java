package com.loglife.nutrition.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for {@code PATCH /api/v1/food-logs/{id}}. Every field is optional; an absent (null)
 * field keeps the stored value. Sending {@code nutrition} overrides the estimated numbers and
 * reclassifies the log as USER_OVERRIDE — no re-estimation happens on edit.
 *
 * @param date        new day of the entry
 * @param mealType    BREAKFAST | LUNCH | DINNER | SNACK | OTHER
 * @param description new free-text description (provenance only; does not trigger estimation)
 * @param quantity    new amount (used together with {@code unit})
 * @param unit        new unit
 * @param notes       new notes ({@code ""} clears them)
 * @param nutrition   corrected nutrition values; all four fields required when present
 */
public record UpdateFoodLogRequest(
        LocalDate date,

        String mealType,

        @Size(min = 2, max = 1000, message = "description must be between 2 and 1000 characters")
        String description,

        @PositiveOrZero(message = "quantity must not be negative")
        BigDecimal quantity,

        @Size(max = 50, message = "unit must be at most 50 characters")
        String unit,

        @Size(max = 2000, message = "notes must be at most 2000 characters")
        String notes,

        @Valid
        NutritionValuesRequest nutrition) {
}
