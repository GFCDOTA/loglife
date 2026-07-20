package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.application.port.out.NutritionGoalRepository;
import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.DailyNutritionSummary;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionFacts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loglife.nutrition.domain.NutritionGoal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDailyNutritionSummaryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 6, 12);

    @Mock
    private FoodLogRepository repository;

    @Mock
    private NutritionGoalRepository goalRepository;

    private GetDailyNutritionSummary useCase() {
        return new GetDailyNutritionSummary(repository, goalRepository);
    }

    @Test
    void aggregatesTotalsAndPerMealBreakdown() {
        when(repository.findByDate(DATE)).thenReturn(List.of(
                log(MealType.BREAKFAST, 300, 15, 40, 8),
                log(MealType.LUNCH, 680, 57, 56, 23),
                log(MealType.LUNCH, 120, 2, 25, 1)));
        when(goalRepository.find()).thenReturn(Optional.empty());

        DailyNutritionSummary summary = useCase().handle(DATE);

        assertThat(summary.date()).isEqualTo(DATE);
        assertThat(summary.totalLogs()).isEqualTo(3);
        assertThat(summary.totals().calories()).isEqualByComparingTo("1100");
        assertThat(summary.totals().proteinGrams()).isEqualByComparingTo("74");
        assertThat(summary.totals().carbsGrams()).isEqualByComparingTo("121");
        assertThat(summary.totals().fatGrams()).isEqualByComparingTo("32");

        assertThat(summary.logsByMealType()).containsOnlyKeys(MealType.BREAKFAST, MealType.LUNCH);
        assertThat(summary.logsByMealType().get(MealType.LUNCH).count()).isEqualTo(2);
        assertThat(summary.logsByMealType().get(MealType.LUNCH).totals().calories()).isEqualByComparingTo("800");
        assertThat(summary.logsByMealType().get(MealType.BREAKFAST).count()).isEqualTo(1);
        assertThat(summary.goalProgress()).isNull();
    }

    @Test
    void emptyDayHasZeroTotals() {
        when(repository.findByDate(DATE)).thenReturn(List.of());
        when(goalRepository.find()).thenReturn(Optional.empty());

        DailyNutritionSummary summary = useCase().handle(DATE);

        assertThat(summary.totalLogs()).isZero();
        assertThat(summary.totals().calories()).isEqualByComparingTo("0");
        assertThat(summary.logsByMealType()).isEmpty();
    }

    @Test
    void goalYieldsRemainingAndPercent() {
        when(repository.findByDate(DATE)).thenReturn(List.of(
                log(MealType.BREAKFAST, 300, 15, 40, 8),
                log(MealType.LUNCH, 700, 50, 60, 20)));
        when(goalRepository.find()).thenReturn(Optional.of(
                new NutritionGoal(BigDecimal.valueOf(2000), BigDecimal.valueOf(150), null, null)));

        DailyNutritionSummary summary = useCase().handle(DATE);

        assertThat(summary.goalProgress()).isNotNull();
        assertThat(summary.goalProgress().goal().calories()).isEqualByComparingTo("2000");
        assertThat(summary.goalProgress().remainingCalories()).isEqualByComparingTo("1000");
        assertThat(summary.goalProgress().percentOfCalories()).isEqualTo(50);
    }

    @Test
    void overshootingTheGoalIsReportedHonestly() {
        when(repository.findByDate(DATE)).thenReturn(List.of(
                log(MealType.DINNER, 2500, 100, 200, 90)));
        when(goalRepository.find()).thenReturn(Optional.of(
                new NutritionGoal(BigDecimal.valueOf(2000), null, null, null)));

        DailyNutritionSummary summary = useCase().handle(DATE);

        assertThat(summary.goalProgress().remainingCalories()).isEqualByComparingTo("-500");
        assertThat(summary.goalProgress().percentOfCalories()).isEqualTo(125);
    }

    private static FoodLog log(MealType mealType, long cal, long protein, long carbs, long fat) {
        Instant now = Instant.parse("2026-06-12T08:00:00Z");
        return FoodLog.reconstitute(
                UUID.randomUUID(), DATE, mealType, "desc", "name",
                FoodQuantity.none(),
                new NutritionFacts(BigDecimal.valueOf(cal), BigDecimal.valueOf(protein),
                        BigDecimal.valueOf(carbs), BigDecimal.valueOf(fat)),
                Confidence.of(0.5), EstimationSource.MOCK, null, null, now, now);
    }
}
