package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.FoodLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Use case: all logs of a date period, ordered by date then creation time — feeds the CSV export.
 */
public class ListFoodLogsByPeriod {

    private final FoodLogRepository repository;

    public ListFoodLogsByPeriod(FoodLogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<FoodLog> handle(LocalDate start, LocalDate end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end must not be before start");
        }
        return repository.findByDateBetween(start, end);
    }
}
