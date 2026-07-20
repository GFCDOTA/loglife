package com.loglife.nutrition.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * API representation of the last-N-days trend. One bucket per day (zeros for empty days);
 * {@code averageCalories} averages only the days that have logs.
 */
public record NutritionTrendResponse(
        List<DayBucket> days,
        int daysWithLogs,
        BigDecimal averageCalories) {

    public record DayBucket(
            LocalDate date,
            int totalLogs,
            BigDecimal calories,
            BigDecimal proteinGrams,
            BigDecimal carbsGrams,
            BigDecimal fatGrams) {
    }
}
