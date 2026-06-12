package com.loglife.nutrition.domain;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read-model: the totals for a single day, plus a per-meal-type breakdown.
 */
public record DailyNutritionSummary(
        LocalDate date,
        NutritionFacts totals,
        int totalLogs,
        Map<MealType, MealTypeSummary> logsByMealType) {

    public DailyNutritionSummary {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(totals, "totals");
        logsByMealType = logsByMealType == null
                ? Map.of()
                : Map.copyOf(logsByMealType);
    }

    /** Per-meal-type subtotal. */
    public record MealTypeSummary(int count, NutritionFacts totals) {
    }

    /** Build the summary by folding the day's logs. */
    public static DailyNutritionSummary fromLogs(LocalDate date, List<FoodLog> logs) {
        Objects.requireNonNull(date, "date");
        List<FoodLog> safe = logs == null ? List.of() : logs;

        NutritionFacts totals = NutritionFacts.zero();
        Map<MealType, MealTypeSummary> byMeal = new EnumMap<>(MealType.class);

        for (FoodLog log : safe) {
            totals = totals.plus(log.nutrition());
            MealTypeSummary current = byMeal.getOrDefault(
                    log.mealType(), new MealTypeSummary(0, NutritionFacts.zero()));
            byMeal.put(log.mealType(),
                    new MealTypeSummary(current.count() + 1, current.totals().plus(log.nutrition())));
        }

        return new DailyNutritionSummary(date, totals, safe.size(), byMeal);
    }
}
