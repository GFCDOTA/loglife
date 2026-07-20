package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.EstimationUnavailableException;
import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.EstimationResult;
import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodDescription;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodLogNotFoundException;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionEstimate;
import com.loglife.nutrition.domain.NutritionFacts;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Re-estimation replaces a low-trust entry (typically MOCK, logged while the real estimator was
 * down) with a fresh estimate of the SAME description — the log keeps its identity and day.
 */
class ReestimateFoodLogTest {

    private static final Instant NOW = Instant.parse("2026-07-20T10:00:00Z");

    private final FoodLogRepository repository = mock(FoodLogRepository.class);
    private final CalorieEstimationPort estimationPort = mock(CalorieEstimationPort.class);
    private final ReestimateFoodLog useCase = new ReestimateFoodLog(
            repository, estimationPort, Clock.fixed(NOW, ZoneOffset.UTC));

    private static FoodLog mockLog() {
        NutritionEstimate estimate = new NutritionEstimate(
                "marmita", FoodQuantity.none(),
                new NutritionFacts(BigDecimal.valueOf(400), BigDecimal.TEN,
                        BigDecimal.valueOf(50), BigDecimal.TEN),
                Confidence.of(0.2), EstimationSource.MOCK, "mock", List.of());
        return FoodLog.create(LocalDate.of(2026, 7, 19), MealType.LUNCH,
                "marmita de frango", FoodQuantity.none(), "obs", estimate,
                Instant.parse("2026-07-19T12:00:00Z"));
    }

    @Test
    void replacesNutritionWithFreshEstimateKeepingIdentity() {
        FoodLog existing = mockLog();
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        NutritionEstimate fresh = new NutritionEstimate(
                "frango grelhado + arroz", FoodQuantity.none(),
                new NutritionFacts(BigDecimal.valueOf(620), BigDecimal.valueOf(45),
                        BigDecimal.valueOf(60), BigDecimal.valueOf(18)),
                Confidence.of(0.8), EstimationSource.OLLAMA, "estimado de novo", List.of());
        when(estimationPort.estimate(any())).thenReturn(EstimationResult.success(fresh));

        FoodLog updated = useCase.handle(existing.id());

        assertThat(updated.id()).isEqualTo(existing.id());
        assertThat(updated.date()).isEqualTo(existing.date());
        assertThat(updated.descriptionOriginal()).isEqualTo("marmita de frango");
        assertThat(updated.nutrition().calories()).isEqualByComparingTo("620");
        assertThat(updated.source()).isEqualTo(EstimationSource.OLLAMA);
        assertThat(updated.confidence().value()).isEqualTo(0.8);
        assertThat(updated.normalizedFoodName()).isEqualTo("frango grelhado + arroz");
        assertThat(updated.notes()).isEqualTo("obs");
        assertThat(updated.createdAt()).isEqualTo(existing.createdAt());
        assertThat(updated.updatedAt()).isEqualTo(NOW);

        ArgumentCaptor<FoodDescription> captor = ArgumentCaptor.forClass(FoodDescription.class);
        verify(estimationPort).estimate(captor.capture());
        assertThat(captor.getValue().rawText()).isEqualTo("marmita de frango");
    }

    @Test
    void failedEstimationLeavesTheLogUntouched() {
        FoodLog existing = mockLog();
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(estimationPort.estimate(any())).thenReturn(EstimationResult.failure("down"));

        assertThatThrownBy(() -> useCase.handle(existing.id()))
                .isInstanceOf(EstimationUnavailableException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void throwsNotFoundForUnknownId() {
        UUID unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(unknown))
                .isInstanceOf(FoodLogNotFoundException.class);
    }
}
