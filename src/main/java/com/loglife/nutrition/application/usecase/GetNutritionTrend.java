package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.NutritionTrend;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Use case: how the last N days looked — per-day totals plus the average over logged days.
 */
public class GetNutritionTrend {

    private final FoodLogRepository repository;

    public GetNutritionTrend(FoodLogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /** {@code reference} is the client's "today"; the window is the {@code days} ending on it. */
    public record Query(LocalDate reference, int days) {

        public Query {
            Objects.requireNonNull(reference, "reference");
            if (days < 1) {
                throw new IllegalArgumentException("days must be at least 1");
            }
        }
    }

    public NutritionTrend handle(Query query) {
        Objects.requireNonNull(query, "query");
        LocalDate start = query.reference().minusDays(query.days() - 1L);
        return NutritionTrend.fromLogs(start, query.reference(),
                repository.findByDateBetween(start, query.reference()));
    }
}
