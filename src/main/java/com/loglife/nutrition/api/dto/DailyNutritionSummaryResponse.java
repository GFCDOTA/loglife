package com.loglife.nutrition.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * API representation of the daily nutrition totals plus a per-meal-type breakdown.
 */
public record DailyNutritionSummaryResponse(
        LocalDate date,
        BigDecimal totalCalories,
        BigDecimal totalProteinGrams,
        BigDecimal totalCarbsGrams,
        BigDecimal totalFatGrams,
        int totalLogs,
        Map<String, MealTypeBucket> logsByMealType) {

    public record MealTypeBucket(
            int count,
            BigDecimal calories,
            BigDecimal proteinGrams,
            BigDecimal carbsGrams,
            BigDecimal fatGrams) {
    }
}
