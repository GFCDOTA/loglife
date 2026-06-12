package com.loglife.nutrition.application.port.out;

import com.loglife.nutrition.domain.FoodLog;

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

    List<FoodLog> findByDate(LocalDate date);

    Optional<FoodLog> findById(UUID id);

    /**
     * @return {@code true} if a row was deleted, {@code false} if no such id existed.
     */
    boolean deleteById(UUID id);
}
