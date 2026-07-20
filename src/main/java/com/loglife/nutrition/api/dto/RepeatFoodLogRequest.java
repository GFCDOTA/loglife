package com.loglife.nutrition.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request body for {@code POST /api/v1/food-logs/{id}/repeat}. The date is the client's day
 * (dates always come from the client); mealType optionally re-buckets the repeated entry.
 */
public record RepeatFoodLogRequest(
        @NotNull(message = "date is required")
        LocalDate date,

        String mealType) {
}
