package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodLogNotFoundException;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionEstimate;
import com.loglife.nutrition.domain.NutritionFacts;
import com.loglife.nutrition.application.port.out.FoodLogRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Repeating an everyday meal clones the persisted nutrition onto a new date — the whole point is
 * paying the LLM zero times for the daily coffee.
 */
class RepeatFoodLogTest {

    private static final Instant NOW = Instant.parse("2026-07-20T09:00:00Z");

    private final FoodLogRepository repository = mock(FoodLogRepository.class);
    private final RepeatFoodLog useCase =
            new RepeatFoodLog(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    private static FoodLog original() {
        NutritionEstimate estimate = new NutritionEstimate(
                "café com leite", FoodQuantity.of(BigDecimal.valueOf(300), "ml"),
                new NutritionFacts(BigDecimal.valueOf(120), BigDecimal.valueOf(6),
                        BigDecimal.valueOf(10), BigDecimal.valueOf(6)),
                Confidence.of(0.8), EstimationSource.OLLAMA, "estimado", List.of());
        return FoodLog.create(LocalDate.of(2026, 7, 19), MealType.BREAKFAST,
                "café com leite de todo dia", FoodQuantity.of(BigDecimal.valueOf(300), "ml"),
                null, estimate, Instant.parse("2026-07-19T08:00:00Z"));
    }

    @Test
    void clonesNutritionOntoNewDateWithoutEstimating() {
        FoodLog source = original();
        when(repository.findById(source.id())).thenReturn(Optional.of(source));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoodLog repeated = useCase.handle(new RepeatFoodLog.Command(
                source.id(), LocalDate.of(2026, 7, 20), null));

        assertThat(repeated.id()).isNotEqualTo(source.id());
        assertThat(repeated.date()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(repeated.mealType()).isEqualTo(MealType.BREAKFAST);
        assertThat(repeated.nutrition()).isEqualTo(source.nutrition());
        assertThat(repeated.quantity()).isEqualTo(source.quantity());
        assertThat(repeated.normalizedFoodName()).isEqualTo(source.normalizedFoodName());
        // Provenance is preserved: the numbers still come from the original estimation.
        assertThat(repeated.source()).isEqualTo(EstimationSource.OLLAMA);
        assertThat(repeated.confidence().value()).isEqualTo(0.8);
        assertThat(repeated.createdAt()).isEqualTo(NOW);
    }

    @Test
    void mealTypeCanBeChangedOnRepeat() {
        FoodLog source = original();
        when(repository.findById(source.id())).thenReturn(Optional.of(source));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoodLog repeated = useCase.handle(new RepeatFoodLog.Command(
                source.id(), LocalDate.of(2026, 7, 20), MealType.SNACK));

        assertThat(repeated.mealType()).isEqualTo(MealType.SNACK);
    }

    @Test
    void throwsNotFoundForUnknownId() {
        UUID unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(new RepeatFoodLog.Command(
                unknown, LocalDate.of(2026, 7, 20), null)))
                .isInstanceOf(FoodLogNotFoundException.class);
    }
}
