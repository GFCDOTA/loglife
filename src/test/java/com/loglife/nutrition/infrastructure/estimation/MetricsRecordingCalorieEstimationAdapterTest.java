package com.loglife.nutrition.infrastructure.estimation;

import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.EstimationResult;
import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodDescription;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionEstimate;
import com.loglife.nutrition.domain.NutritionFacts;
import com.loglife.nutrition.infrastructure.observability.NutritionMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsRecordingCalorieEstimationAdapterTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final NutritionMetrics metrics = new NutritionMetrics(registry);

    private static final FoodDescription DESCRIPTION = new FoodDescription(
            "200g de arroz", LocalDate.of(2026, 6, 12), MealType.LUNCH, FoodQuantity.none(), "pt-BR");

    @Test
    void recordsEstimateTaggedBySourceOnSuccess() {
        NutritionEstimate estimate = new NutritionEstimate(
                "arroz cozido", FoodQuantity.none(),
                new NutritionFacts(BigDecimal.valueOf(260), BigDecimal.ZERO, BigDecimal.valueOf(56), BigDecimal.ZERO),
                Confidence.of(0.5), EstimationSource.MOCK, "ok", List.of());
        CalorieEstimationPort delegate = description -> EstimationResult.success(estimate);

        new MetricsRecordingCalorieEstimationAdapter(delegate, metrics).estimate(DESCRIPTION);

        assertThat(registry.get("loglife.nutrition.estimates").tag("source", "MOCK").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void recordsNothingOnFailure() {
        CalorieEstimationPort delegate = description -> EstimationResult.failure("down");

        EstimationResult result = new MetricsRecordingCalorieEstimationAdapter(delegate, metrics).estimate(DESCRIPTION);

        assertThat(result.isFailure()).isTrue();
        Counter c = registry.find("loglife.nutrition.estimates").counter();
        assertThat(c).isNull();
    }
}
