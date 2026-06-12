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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeCalorieEstimationAdapterTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final NutritionMetrics metrics = new NutritionMetrics(registry);
    private final MockCalorieEstimationAdapter fallback = new MockCalorieEstimationAdapter();

    private static final FoodDescription DESCRIPTION = new FoodDescription(
            "2 bifes médios e 200g de arroz", LocalDate.of(2026, 6, 12), MealType.LUNCH,
            FoodQuantity.none(), "pt-BR");

    @Test
    void usesFallbackAndRecordsMetricsWhenPrimaryFails() {
        CalorieEstimationPort primary = description -> EstimationResult.failure("agent offline");
        CompositeCalorieEstimationAdapter composite =
                new CompositeCalorieEstimationAdapter(primary, fallback, metrics);

        EstimationResult result = composite.estimate(DESCRIPTION);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.estimate()).get()
                .extracting(NutritionEstimate::source)
                .isEqualTo(EstimationSource.MOCK);

        assertThat(counter("loglife.nutrition.local_agent.failures")).isEqualTo(1.0);
        assertThat(counter("loglife.nutrition.estimation.fallbacks")).isEqualTo(1.0);
        assertThat(sourceCounter("MOCK")).isEqualTo(1.0);
    }

    @Test
    void usesPrimaryWhenItSucceedsAndDoesNotFallBack() {
        NutritionEstimate primaryEstimate = new NutritionEstimate(
                "arroz cozido", FoodQuantity.none(),
                new NutritionFacts(BigDecimal.valueOf(260), BigDecimal.valueOf(5),
                        BigDecimal.valueOf(56), BigDecimal.valueOf(1)),
                Confidence.of(0.85), EstimationSource.LOCAL_AGENT, "ok", List.of());
        CalorieEstimationPort primary = description -> EstimationResult.success(primaryEstimate);
        CompositeCalorieEstimationAdapter composite =
                new CompositeCalorieEstimationAdapter(primary, fallback, metrics);

        EstimationResult result = composite.estimate(DESCRIPTION);

        assertThat(result.estimate()).get()
                .extracting(NutritionEstimate::source)
                .isEqualTo(EstimationSource.LOCAL_AGENT);
        assertThat(counter("loglife.nutrition.local_agent.failures")).isEqualTo(0.0);
        assertThat(counter("loglife.nutrition.estimation.fallbacks")).isEqualTo(0.0);
        assertThat(sourceCounter("LOCAL_AGENT")).isEqualTo(1.0);
    }

    @Test
    void treatsThrowingPrimaryAsFailure() {
        CalorieEstimationPort primary = description -> {
            throw new IllegalStateException("boom");
        };
        CompositeCalorieEstimationAdapter composite =
                new CompositeCalorieEstimationAdapter(primary, fallback, metrics);

        EstimationResult result = composite.estimate(DESCRIPTION);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.estimate()).get()
                .extracting(NutritionEstimate::source)
                .isEqualTo(EstimationSource.MOCK);
        assertThat(counter("loglife.nutrition.local_agent.failures")).isEqualTo(1.0);
    }

    private double counter(String name) {
        return registry.get(name).counter().count();
    }

    private double sourceCounter(String source) {
        return registry.get("loglife.nutrition.estimates").tag("source", source).counter().count();
    }
}
