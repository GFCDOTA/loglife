package com.loglife.nutrition.application.port.out;

import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FrequentFood;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting and querying {@link FoodLog}s. The application core depends on
 * this interface only; the concrete JPA adapter lives in the infrastructure layer.
 */
public interface FoodLogRepository {

    FoodLog save(FoodLog foodLog);

    /**
     * Persist several logs as a single unit of work — used when one free-text entry is split into
     * one log per food item. Implementations must make this atomic (all rows or none).
     */
    List<FoodLog> saveAll(List<FoodLog> foodLogs);

    List<FoodLog> findByDate(LocalDate date);

    /** All logs with {@code start <= date <= end}, ordered by date then creation time. */
    List<FoodLog> findByDateBetween(LocalDate start, LocalDate end);

    Optional<FoodLog> findById(UUID id);

    /**
     * @return {@code true} if a row was deleted, {@code false} if no such id existed.
     */
    boolean deleteById(UUID id);

    /**
     * The foods logged most often since {@code since} (inclusive), most frequent first, at most
     * {@code limit} entries. Each entry carries the LATEST log of that food, whose nutrition is
     * what a "repeat" clones.
     */
    List<FrequentFood> findFrequentSince(LocalDate since, int limit);
}
