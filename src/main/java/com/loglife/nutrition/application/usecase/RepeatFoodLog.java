package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodLogNotFoundException;
import com.loglife.nutrition.domain.MealType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Use case: log an everyday meal again by cloning an existing entry onto a new date. The
 * persisted nutrition is reused as-is — zero estimator (LLM) round-trips.
 */
public class RepeatFoodLog {

    private static final Logger log = LoggerFactory.getLogger(RepeatFoodLog.class);

    private final FoodLogRepository repository;
    private final Clock clock;

    public RepeatFoodLog(FoodLogRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** {@code mealType} is optional: null keeps the original meal type. */
    public record Command(UUID sourceId, LocalDate date, MealType mealType) {

        public Command {
            Objects.requireNonNull(sourceId, "sourceId");
            Objects.requireNonNull(date, "date");
        }
    }

    @Transactional
    public FoodLog handle(Command command) {
        Objects.requireNonNull(command, "command");
        FoodLog source = repository.findById(command.sourceId())
                .orElseThrow(() -> new FoodLogNotFoundException(command.sourceId()));

        FoodLog repeated = repository.save(
                source.repeatedOn(command.date(), command.mealType(), clock.instant()));
        log.info("Food log repeated sourceId={} newId={} date={}",
                command.sourceId(), repeated.id(), repeated.date());
        return repeated;
    }
}
