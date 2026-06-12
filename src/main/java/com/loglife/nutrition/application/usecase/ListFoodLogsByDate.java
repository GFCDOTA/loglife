package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.FoodLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Use case: list all food logs recorded on a given day.
 */
public class ListFoodLogsByDate {

    private final FoodLogRepository repository;

    public ListFoodLogsByDate(FoodLogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<FoodLog> handle(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return repository.findByDate(date);
    }
}
