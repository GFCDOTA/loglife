package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.NutritionGoalRepository;
import com.loglife.nutrition.domain.NutritionGoal;

import java.util.Objects;
import java.util.Optional;

/**
 * Use case: read the user's daily nutrition goal, if one has been set.
 */
public class GetNutritionGoal {

    private final NutritionGoalRepository repository;

    public GetNutritionGoal(NutritionGoalRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Optional<NutritionGoal> handle() {
        return repository.find();
    }
}
