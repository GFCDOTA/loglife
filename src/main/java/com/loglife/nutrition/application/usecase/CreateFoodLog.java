package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.EstimationUnavailableException;
import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.EstimationResult;
import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodDescription;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionEstimate;
import com.loglife.nutrition.domain.NutritionFacts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
            String language,
            NutritionFacts nutrition) {
    }

    /**
     * Estimate the nutrition of a free-text entry and persist it. When the estimate breaks the
     * text into several food items ("bife + ovo + pão"), this creates ONE {@link FoodLog} per item
     * so the day's list and totals are itemised; otherwise it creates a single aggregate log.
     * Either way exactly one of the two representations is stored, never both, so daily totals are
     * never double-counted. All rows are persisted atomically.
     *
     * @return the persisted logs (at least one)
     */
    public List<FoodLog> handle(Command command) {
        Objects.requireNonNull(command, "command");

        if (command.nutrition() != null) {
            // The user typed the values from a label: no estimation, full confidence, MANUAL.
            log.info("Creating manual food log date={} mealType={}", command.date(), command.mealType());
            NutritionEstimate manual = new NutritionEstimate(
                    command.description(), command.quantity(), command.nutrition(),
                    Confidence.of(1.0), EstimationSource.MANUAL, null, List.of());
            List<FoodLog> saved = repository.saveAll(List.of(FoodLog.create(
                    command.date(), command.mealType(), command.description(),
                    command.quantity(), command.notes(), manual, clock.instant())));
            log.info("Food log created count=1 source=MANUAL totalCalories={}",
                    command.nutrition().calories());
            return saved;
        }

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

        List<FoodLog> toSave;
        if (!estimate.items().isEmpty()) {
            // One log per food item — the description is kept on each as provenance.
            toSave = estimate.items().stream()
                    .map(item -> FoodLog.fromItem(
                            command.date(), command.mealType(), command.description(),
                            item, estimate.source(), estimate.explanation(), command.notes(), now))
                    .toList();
        } else {
            // No line items (e.g. the agent returned only a total): keep one aggregate log.
            toSave = List.of(FoodLog.create(
                    command.date(), command.mealType(), command.description(),
                    command.quantity(), command.notes(), estimate, now));
        }

        List<FoodLog> saved = repository.saveAll(toSave);
        log.info("Food log(s) created count={} source={} totalCalories={} confidence={}",
                saved.size(), estimate.source(), estimate.nutrition().calories(), estimate.confidence().value());
        return saved;
    }
}
