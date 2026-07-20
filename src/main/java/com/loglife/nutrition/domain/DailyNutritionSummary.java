package com.loglife.nutrition.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        Map<MealType, MealTypeSummary> logsByMealType,
        GoalProgress goalProgress) {

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

    /**
     * Where the day stands against the user's goal. {@code remainingCalories} goes negative and
     * {@code percentOfCalories} passes 100 on overshoot — the numbers are honest, the UI decides
     * how to render them.
     */
    public record GoalProgress(NutritionGoal goal, BigDecimal remainingCalories, int percentOfCalories) {

        public static GoalProgress of(NutritionGoal goal, NutritionFacts totals) {
            Objects.requireNonNull(goal, "goal");
            BigDecimal consumed = totals.calories();
            BigDecimal remaining = goal.calories().subtract(consumed);
            int percent = consumed.multiply(BigDecimal.valueOf(100))
                    .divide(goal.calories(), 0, RoundingMode.HALF_UP)
                    .intValueExact();
            return new GoalProgress(goal, remaining, percent);
        }
    }

    /** Build the summary by folding the day's logs; no goal configured. */
    public static DailyNutritionSummary fromLogs(LocalDate date, List<FoodLog> logs) {
        return fromLogs(date, logs, null);
    }

    /** Build the summary by folding the day's logs, scored against the goal when one exists. */
    public static DailyNutritionSummary fromLogs(LocalDate date, List<FoodLog> logs, NutritionGoal goal) {
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

        GoalProgress progress = goal == null ? null : GoalProgress.of(goal, totals);
        return new DailyNutritionSummary(date, totals, safe.size(), byMeal, progress);
    }
}
