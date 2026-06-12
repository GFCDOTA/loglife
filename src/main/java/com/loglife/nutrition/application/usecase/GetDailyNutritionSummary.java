package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.DailyNutritionSummary;
import com.loglife.nutrition.domain.FoodLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Use case: compute the nutrition totals (and per-meal breakdown) for a given day.
 */
public class GetDailyNutritionSummary {

    private final FoodLogRepository repository;

    public GetDailyNutritionSummary(FoodLogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public DailyNutritionSummary handle(LocalDate date) {
        Objects.requireNonNull(date, "date");
        List<FoodLog> logs = repository.findByDate(date);
        return DailyNutritionSummary.fromLogs(date, logs);
    }
}
