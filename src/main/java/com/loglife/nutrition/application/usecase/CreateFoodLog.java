package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.EstimationUnavailableException;
import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.EstimationResult;
import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.FoodDescription;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionEstimate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Use case: register a food/meal entry from free text. It asks a {@link CalorieEstimationPort}
 * (backed by the user's local agents) to estimate the nutrition, then persists a {@link FoodLog}.
 */
public class CreateFoodLog {

    private static final Logger log = LoggerFactory.getLogger(CreateFoodLog.class);

    private final CalorieEstimationPort estimationPort;
    private final FoodLogRepository repository;
    private final Clock clock;

    public CreateFoodLog(CalorieEstimationPort estimationPort, FoodLogRepository repository, Clock clock) {
        this.estimationPort = Objects.requireNonNull(estimationPort, "estimationPort");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public record Command(
            LocalDate date,
            MealType mealType,
            String description,
            FoodQuantity quantity,
            String notes,
            String language) {
    }

    public FoodLog handle(Command command) {
        Objects.requireNonNull(command, "command");

        FoodDescription description = new FoodDescription(
                command.description(), command.date(), command.mealType(),
                command.quantity(), command.language());

        // Privacy: never log the raw food text at INFO; only structural fields.
        log.info("Creating food log date={} mealType={}", command.date(), command.mealType());
        log.debug("Estimating nutrition for rawText='{}'", description.rawText());

        EstimationResult result = estimationPort.estimate(description);
        NutritionEstimate estimate = result.estimate()
                .orElseThrow(() -> new EstimationUnavailableException(result.failureReason()));

        Instant now = clock.instant();
        FoodLog foodLog = FoodLog.create(
                command.date(), command.mealType(), command.description(),
                command.quantity(), command.notes(), estimate, now);

        FoodLog saved = repository.save(foodLog);
        log.info("Food log created id={} source={} calories={} confidence={}",
                saved.id(), saved.source(), saved.nutrition().calories(), saved.confidence().value());
        return saved;
    }
}
