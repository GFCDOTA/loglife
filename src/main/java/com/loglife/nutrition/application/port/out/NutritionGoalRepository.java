package com.loglife.nutrition.application.port.out;

import com.loglife.nutrition.domain.NutritionGoal;

import java.util.Optional;

/**
 * Persistence port for the user's daily nutrition goal. Single-user app: there is at most one
 * goal; {@link #save} replaces it.
 */
public interface NutritionGoalRepository {

    Optional<NutritionGoal> find();

    NutritionGoal save(NutritionGoal goal);
}
