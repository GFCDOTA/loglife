package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.NutritionGoalRepository;
import com.loglife.nutrition.domain.NutritionGoal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Use case: set (or replace) the user's daily nutrition goal. Single-user: this is an upsert of
 * the one goal row.
 */
public class SetNutritionGoal {

    private static final Logger log = LoggerFactory.getLogger(SetNutritionGoal.class);

    private final NutritionGoalRepository repository;

    public SetNutritionGoal(NutritionGoalRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public NutritionGoal handle(NutritionGoal goal) {
        Objects.requireNonNull(goal, "goal");
        NutritionGoal saved = repository.save(goal);
        log.info("Daily goal set calories={}", saved.calories());
        return saved;
    }
}
