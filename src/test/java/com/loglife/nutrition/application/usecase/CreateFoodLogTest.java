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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFoodLogTest {

    private static final Instant NOW = Instant.parse("2026-06-12T10:15:30Z");
    private final Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private CalorieEstimationPort estimationPort;

    @Mock
    private FoodLogRepository repository;

    private CreateFoodLog createFoodLog() {
        return new CreateFoodLog(estimationPort, repository, fixedClock);
    }

    @Test
    void estimatesNutritionAndPersistsFoodLog() {
        NutritionEstimate estimate = new NutritionEstimate(
                "bife bovino grelhado + arroz cozido",
                FoodQuantity.none(),
                new NutritionFacts(BigDecimal.valueOf(680), BigDecimal.valueOf(57),
                        BigDecimal.valueOf(56), BigDecimal.valueOf(23)),
                Confidence.of(0.78),
                EstimationSource.LOCAL_AGENT,
                "Estimativa baseada nos alimentos informados.",
                List.of());
        when(estimationPort.estimate(any())).thenReturn(EstimationResult.success(estimate));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateFoodLog.Command command = new CreateFoodLog.Command(
                LocalDate.of(2026, 6, 12), MealType.LUNCH,
                "2 bifes médios e 200g de arroz", FoodQuantity.none(), "almoço", "pt-BR");

        FoodLog saved = createFoodLog().handle(command);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.date()).isEqualTo(LocalDate.of(2026, 6, 12));
        assertThat(saved.mealType()).isEqualTo(MealType.LUNCH);
        assertThat(saved.descriptionOriginal()).isEqualTo("2 bifes médios e 200g de arroz");
        assertThat(saved.normalizedFoodName()).isEqualTo("bife bovino grelhado + arroz cozido");
        assertThat(saved.nutrition().calories()).isEqualByComparingTo("680");
        assertThat(saved.nutrition().proteinGrams()).isEqualByComparingTo("57");
        assertThat(saved.source()).isEqualTo(EstimationSource.LOCAL_AGENT);
        assertThat(saved.confidence().value()).isEqualTo(0.78);
        assertThat(saved.createdAt()).isEqualTo(NOW);
        assertThat(saved.updatedAt()).isEqualTo(NOW);

        ArgumentCaptor<FoodDescription> captor = ArgumentCaptor.forClass(FoodDescription.class);
        verify(estimationPort).estimate(captor.capture());
        assertThat(captor.getValue().rawText()).isEqualTo("2 bifes médios e 200g de arroz");
        assertThat(captor.getValue().mealType()).isEqualTo(MealType.LUNCH);
        assertThat(captor.getValue().language()).isEqualTo("pt-BR");
    }

    @Test
    void throwsWhenEstimationFails() {
        when(estimationPort.estimate(any())).thenReturn(EstimationResult.failure("all estimators down"));

        CreateFoodLog.Command command = new CreateFoodLog.Command(
                LocalDate.of(2026, 6, 12), MealType.DINNER,
                "sopa de legumes", FoodQuantity.none(), null, null);

        assertThatThrownBy(() -> createFoodLog().handle(command))
                .isInstanceOf(EstimationUnavailableException.class);

        verify(repository, never()).save(any());
    }
}
