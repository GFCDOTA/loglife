package com.loglife.nutrition.application.usecase;

import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionFacts;
import com.loglife.nutrition.domain.NutritionTrend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * The 7-day trend answers "how have I been eating" — one bucket per day (zeros for empty days,
 * so the chart has no holes) plus the average over days that actually have logs.
 */
@ExtendWith(MockitoExtension.class)
class GetNutritionTrendTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 20);

    @Mock
    private FoodLogRepository repository;

    @Test
    void buildsOneBucketPerDayIncludingEmptyDays() {
        when(repository.findByDateBetween(TODAY.minusDays(6), TODAY)).thenReturn(List.of(
                log(TODAY.minusDays(6), 1800),
                log(TODAY.minusDays(6), 200),
                log(TODAY, 1500)));

        NutritionTrend trend = new GetNutritionTrend(repository)
                .handle(new GetNutritionTrend.Query(TODAY, 7));

        assertThat(trend.days()).hasSize(7);
        assertThat(trend.days().get(0).date()).isEqualTo(TODAY.minusDays(6));
        assertThat(trend.days().get(0).totals().calories()).isEqualByComparingTo("2000");
        assertThat(trend.days().get(1).totals().calories()).isEqualByComparingTo("0");
        assertThat(trend.days().get(6).date()).isEqualTo(TODAY);
        assertThat(trend.days().get(6).totals().calories()).isEqualByComparingTo("1500");
    }

    @Test
    void averagesOnlyOverDaysWithLogs() {
        when(repository.findByDateBetween(TODAY.minusDays(6), TODAY)).thenReturn(List.of(
                log(TODAY.minusDays(6), 2000),
                log(TODAY, 1500)));

        NutritionTrend trend = new GetNutritionTrend(repository)
                .handle(new GetNutritionTrend.Query(TODAY, 7));

        // (2000 + 1500) / 2 logged days — five empty days do NOT drag the average to zero.
        assertThat(trend.daysWithLogs()).isEqualTo(2);
        assertThat(trend.averageCalories()).isEqualByComparingTo("1750");
    }

    @Test
    void emptyWindowHasZeroAverage() {
        when(repository.findByDateBetween(TODAY.minusDays(6), TODAY)).thenReturn(List.of());

        NutritionTrend trend = new GetNutritionTrend(repository)
                .handle(new GetNutritionTrend.Query(TODAY, 7));

        assertThat(trend.daysWithLogs()).isZero();
        assertThat(trend.averageCalories()).isEqualByComparingTo("0");
    }

    private static FoodLog log(LocalDate date, long calories) {
        Instant now = Instant.parse("2026-07-20T08:00:00Z");
        return FoodLog.reconstitute(
                UUID.randomUUID(), date, MealType.LUNCH, "desc", "name",
                FoodQuantity.none(),
                new NutritionFacts(BigDecimal.valueOf(calories), BigDecimal.ONE,
                        BigDecimal.ONE, BigDecimal.ONE),
                Confidence.of(0.5), EstimationSource.MOCK, null, null, now, now);
    }
}
