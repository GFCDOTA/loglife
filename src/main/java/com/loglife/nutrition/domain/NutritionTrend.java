package com.loglife.nutrition.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Read-model: daily totals over a window of days (one bucket per day, zeros for days without
 * logs, so a chart has no holes) plus the calorie average over the days that DO have logs —
 * empty days are missing data, not zero-calorie days.
 */
public record NutritionTrend(
        List<DayBucket> days,
        int daysWithLogs,
        BigDecimal averageCalories) {

    public NutritionTrend {
        days = days == null ? List.of() : List.copyOf(days);
        Objects.requireNonNull(averageCalories, "averageCalories");
    }

    /** One day of the window. */
    public record DayBucket(LocalDate date, int totalLogs, NutritionFacts totals) {
    }

    /** Fold a window of logs into per-day buckets from {@code start} to {@code end} inclusive. */
    public static NutritionTrend fromLogs(LocalDate start, LocalDate end, List<FoodLog> logs) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end must not be before start");
        }
        Map<LocalDate, List<FoodLog>> byDate = (logs == null ? List.<FoodLog>of() : logs).stream()
                .collect(Collectors.groupingBy(FoodLog::date));

        List<DayBucket> buckets = new ArrayList<>();
        BigDecimal caloriesSum = BigDecimal.ZERO;
        int daysWithLogs = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            List<FoodLog> dayLogs = byDate.getOrDefault(day, List.of());
            NutritionFacts totals = dayLogs.stream()
                    .map(FoodLog::nutrition)
                    .reduce(NutritionFacts.zero(), NutritionFacts::plus);
            buckets.add(new DayBucket(day, dayLogs.size(), totals));
            if (!dayLogs.isEmpty()) {
                daysWithLogs++;
                caloriesSum = caloriesSum.add(totals.calories());
            }
        }

        BigDecimal average = daysWithLogs == 0
                ? BigDecimal.ZERO
                : caloriesSum.divide(BigDecimal.valueOf(daysWithLogs), 2, RoundingMode.HALF_UP);
        return new NutritionTrend(buckets, daysWithLogs, average);
    }
}
