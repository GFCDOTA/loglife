package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.EstimationUnavailableException;
import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.EstimationResult;
import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.FoodDescription;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodLogNotFoundException;
import com.loglife.nutrition.domain.NutritionEstimate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * Use case: run the estimator again over an existing log's original description and replace the
 * numbers in place — upgrades MOCK entries once the real estimator is back. NOT annotated
 * {@code @Transactional} on purpose: like CreateFoodLog, the LLM call (up to ~2 min cold) must
 * never hold a database transaction open; the final save is a single atomic write.
 */
public class ReestimateFoodLog {

    private static final Logger log = LoggerFactory.getLogger(ReestimateFoodLog.class);

    private final FoodLogRepository repository;
    private final CalorieEstimationPort estimationPort;
    private final Clock clock;

    public ReestimateFoodLog(FoodLogRepository repository, CalorieEstimationPort estimationPort,
                             Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.estimationPort = Objects.requireNonNull(estimationPort, "estimationPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public FoodLog handle(UUID id) {
        Objects.requireNonNull(id, "id");
        FoodLog existing = repository.findById(id)
                .orElseThrow(() -> new FoodLogNotFoundException(id));

        EstimationResult result = estimationPort.estimate(new FoodDescription(
                existing.descriptionOriginal(), existing.date(), existing.mealType(),
                existing.quantity(), "pt-BR"));
        NutritionEstimate estimate = result.estimate()
                .orElseThrow(() -> new EstimationUnavailableException(result.failureReason()));

        FoodLog updated = repository.save(existing.withFreshEstimate(estimate, clock.instant()));
        log.info("Food log re-estimated id={} source {} -> {}",
                id, existing.source(), updated.source());
        return updated;
    }
}
