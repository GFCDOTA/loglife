package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.FoodLogNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * Use case: delete a food log created by mistake.
 */
public class DeleteFoodLog {

    private static final Logger log = LoggerFactory.getLogger(DeleteFoodLog.class);

    private final FoodLogRepository repository;

    public DeleteFoodLog(FoodLogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void handle(UUID id) {
        Objects.requireNonNull(id, "id");
        boolean deleted = repository.deleteById(id);
        if (!deleted) {
            throw new FoodLogNotFoundException(id);
        }
        log.info("Food log deleted id={}", id);
    }
}
