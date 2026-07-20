package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodLogNotFoundException;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionFacts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Use case: correct an existing food log WITHOUT re-estimating — a wrong entry no longer forces
 * delete + re-create (and a second LLM round-trip). Null command fields mean "keep". Editing the
 * nutrition reclassifies the log as USER_OVERRIDE with full confidence (see
 * {@link FoodLog#withUserEdits}).
 *
 * <p>This use case is the transactional boundary: load + save must be atomic and there is no
 * long-running estimation call inside (unlike CreateFoodLog, whose LLM call cannot hold a
 * transaction open).
 */
public class UpdateFoodLog {

    private static final Logger log = LoggerFactory.getLogger(UpdateFoodLog.class);

    private final FoodLogRepository repository;
    private final Clock clock;

    public UpdateFoodLog(FoodLogRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public record Command(
            UUID id,
            LocalDate date,
            MealType mealType,
            String description,
            FoodQuantity quantity,
            String notes,
            NutritionFacts nutrition) {

        public Command {
            Objects.requireNonNull(id, "id");
        }
    }

    @Transactional
    public FoodLog handle(Command command) {
        Objects.requireNonNull(command, "command");
        FoodLog existing = repository.findById(command.id())
                .orElseThrow(() -> new FoodLogNotFoundException(command.id()));

        FoodLog updated = existing.withUserEdits(
                command.date(), command.mealType(), command.description(),
                command.quantity(), command.notes(), command.nutrition(),
                clock.instant());

        FoodLog saved = repository.save(updated);
        log.info("Food log updated id={} nutritionOverridden={} source={}",
                saved.id(), command.nutrition() != null, saved.source());
        return saved;
    }
}
