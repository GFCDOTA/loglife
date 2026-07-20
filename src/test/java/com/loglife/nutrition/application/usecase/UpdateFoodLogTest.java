package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodLogNotFoundException;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionEstimate;
import com.loglife.nutrition.domain.NutritionFacts;
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
 * The edit use case must never re-estimate: it loads, applies the user's changes and saves.
 * Editing nutrition numbers reclassifies the log as USER_OVERRIDE with full confidence; editing
 * only metadata (notes, meal type) keeps the original estimation provenance.
 */
class UpdateFoodLogTest {

    private static final Instant ORIGINAL_TIME = Instant.parse("2026-06-12T12:00:00Z");
    private static final Instant EDIT_TIME = Instant.parse("2026-06-13T08:30:00Z");

    private final FoodLogRepository repository = mock(FoodLogRepository.class);
    private final Clock clock = Clock.fixed(EDIT_TIME, ZoneOffset.UTC);
    private final UpdateFoodLog useCase = new UpdateFoodLog(repository, clock);

    private static FoodLog existingLog() {
        NutritionEstimate estimate = new NutritionEstimate(
                "arroz cozido",
                FoodQuantity.of(BigDecimal.valueOf(200), "g"),
                new NutritionFacts(BigDecimal.valueOf(260), BigDecimal.valueOf(5),
                        BigDecimal.valueOf(56), BigDecimal.valueOf(1)),
                Confidence.of(0.7), EstimationSource.OLLAMA, "estimativa por porção", List.of());
        return FoodLog.create(LocalDate.of(2026, 6, 12), MealType.LUNCH, "200g de arroz",
                FoodQuantity.of(BigDecimal.valueOf(200), "g"), null, estimate, ORIGINAL_TIME);
    }

    @Test
    void overridingNutritionBecomesUserOverrideWithFullConfidence() {
        FoodLog existing = existingLog();
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoodLog updated = useCase.handle(new UpdateFoodLog.Command(
                existing.id(), null, null, null, null, null,
                new NutritionFacts(BigDecimal.valueOf(300), BigDecimal.valueOf(6),
                        BigDecimal.valueOf(60), BigDecimal.valueOf(2))));

        assertThat(updated.nutrition().calories()).isEqualByComparingTo("300");
        assertThat(updated.source()).isEqualTo(EstimationSource.USER_OVERRIDE);
        assertThat(updated.confidence().value()).isEqualTo(1.0);
        assertThat(updated.updatedAt()).isEqualTo(EDIT_TIME);
        assertThat(updated.createdAt()).isEqualTo(ORIGINAL_TIME);
        assertThat(updated.id()).isEqualTo(existing.id());
    }

    @Test
    void editingOnlyMetadataKeepsEstimationProvenance() {
        FoodLog existing = existingLog();
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoodLog updated = useCase.handle(new UpdateFoodLog.Command(
                existing.id(), null, MealType.DINNER, null, null, "na verdade era janta", null));

        assertThat(updated.mealType()).isEqualTo(MealType.DINNER);
        assertThat(updated.notes()).isEqualTo("na verdade era janta");
        assertThat(updated.source()).isEqualTo(EstimationSource.OLLAMA);
        assertThat(updated.confidence().value()).isEqualTo(0.7);
        assertThat(updated.nutrition().calories()).isEqualByComparingTo("260");
        assertThat(updated.descriptionOriginal()).isEqualTo("200g de arroz");
    }

    @Test
    void untouchedFieldsSurviveTheEdit() {
        FoodLog existing = existingLog();
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoodLog updated = useCase.handle(new UpdateFoodLog.Command(
                existing.id(), null, null, null, null, null,
                new NutritionFacts(BigDecimal.valueOf(300), BigDecimal.valueOf(6),
                        BigDecimal.valueOf(60), BigDecimal.valueOf(2))));

        assertThat(updated.date()).isEqualTo(existing.date());
        assertThat(updated.mealType()).isEqualTo(existing.mealType());
        assertThat(updated.descriptionOriginal()).isEqualTo(existing.descriptionOriginal());
        assertThat(updated.quantity()).isEqualTo(existing.quantity());
        assertThat(updated.normalizedFoodName()).isEqualTo(existing.normalizedFoodName());
    }

    @Test
    void throwsNotFoundForUnknownId() {
        UUID unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(new UpdateFoodLog.Command(
                unknown, null, null, null, null, "x", null)))
                .isInstanceOf(FoodLogNotFoundException.class);
    }
}
