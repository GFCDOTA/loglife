package com.loglife.nutrition.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for {@code POST /api/v1/food-logs}.
 *
 * @param date        the day of the entry; omitted = "today" in the server's configured timezone
 *                    (voice/shortcut entries can skip it)
 * @param mealType    BREAKFAST | LUNCH | DINNER | SNACK | OTHER (required)
 * @param description free-text food description, e.g. "2 bifes médios e 200g de arroz" (required, min 2 chars)
 * @param quantity   optional amount
 * @param unit       optional unit
 * @param notes      optional free-text notes
 * @param language   optional BCP-47 language tag; defaults to pt-BR
 * @param nutrition  optional label values; when present the LLM is skipped entirely (source MANUAL)
 */
public record CreateFoodLogRequest(
        LocalDate date,

        @NotBlank(message = "mealType is required")
        String mealType,

        @NotBlank(message = "description is required")
        @Size(min = 2, max = 1000, message = "description must be between 2 and 1000 characters")
        String description,

        @PositiveOrZero(message = "quantity must not be negative")
        BigDecimal quantity,

        @Size(max = 50, message = "unit must be at most 50 characters")
        String unit,

        @Size(max = 2000, message = "notes must be at most 2000 characters")
        String notes,

        String language,

        @Valid
        NutritionValuesRequest nutrition) {
}
