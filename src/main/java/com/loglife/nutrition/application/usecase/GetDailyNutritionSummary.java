package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.application.port.out.NutritionGoalRepository;
import com.loglife.nutrition.domain.DailyNutritionSummary;
import com.loglife.nutrition.domain.FoodLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Use case: compute the nutrition totals (and per-meal breakdown) for a given day, scored
 * against the daily goal when one is configured.
 */
public class GetDailyNutritionSummary {

    private final FoodLogRepository repository;
    private final NutritionGoalRepository goalRepository;

    public GetDailyNutritionSummary(FoodLogRepository repository, NutritionGoalRepository goalRepository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.goalRepository = Objects.requireNonNull(goalRepository, "goalRepository");
    }

    public DailyNutritionSummary handle(LocalDate date) {
        Objects.requireNonNull(date, "date");
        List<FoodLog> logs = repository.findByDate(date);
        return DailyNutritionSummary.fromLogs(date, logs, goalRepository.find().orElse(null));
    }
}
