package com.loglife.nutrition.infrastructure.persistence;

import com.loglife.nutrition.application.port.out.NutritionGoalRepository;
import com.loglife.nutrition.domain.NutritionGoal;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.Optional;

/**
 * JPA-backed implementation of the {@link NutritionGoalRepository} port. Saving always writes the
 * singleton row (id = 1), so "set the goal" is an upsert.
 */
@Repository
public class NutritionGoalRepositoryAdapter implements NutritionGoalRepository {

    private final SpringDataUserGoalRepository jpa;
    private final Clock clock;

    public NutritionGoalRepositoryAdapter(SpringDataUserGoalRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public Optional<NutritionGoal> find() {
        return jpa.findById(UserGoalJpaEntity.SINGLETON_ID).map(NutritionGoalRepositoryAdapter::toDomain);
    }

    @Override
    public NutritionGoal save(NutritionGoal goal) {
        UserGoalJpaEntity entity = new UserGoalJpaEntity(
                goal.calories(), goal.proteinGrams(), goal.carbsGrams(), goal.fatGrams(),
                clock.instant());
        return toDomain(jpa.save(entity));
    }

    private static NutritionGoal toDomain(UserGoalJpaEntity entity) {
        return new NutritionGoal(entity.getCalories(), entity.getProteinGrams(),
                entity.getCarbsGrams(), entity.getFatGrams());
    }
}
