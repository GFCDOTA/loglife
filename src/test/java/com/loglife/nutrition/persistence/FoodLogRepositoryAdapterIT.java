package com.loglife.nutrition.persistence;

import com.loglife.AbstractPostgresIntegrationTest;
import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionEstimate;
import com.loglife.nutrition.domain.NutritionFacts;
import com.loglife.nutrition.infrastructure.persistence.FoodLogRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence test for {@link FoodLogRepositoryAdapter} against a real PostgreSQL, exercising the
 * Flyway-created schema (the migration is validated by Hibernate on boot).
 */
class FoodLogRepositoryAdapterIT extends AbstractPostgresIntegrationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 6, 12);

    @Autowired
    private FoodLogRepositoryAdapter adapter;

    @Test
    void savesAndReadsBackById() {
        FoodLog saved = adapter.save(sampleLog(DATE, MealType.LUNCH));

        assertThat(adapter.findById(saved.id())).get().satisfies(found -> {
            assertThat(found.descriptionOriginal()).isEqualTo("2 bifes médios e 200g de arroz");
            assertThat(found.nutrition().calories()).isEqualByComparingTo("680");
            assertThat(found.source()).isEqualTo(EstimationSource.LOCAL_AGENT);
            assertThat(found.confidence().value()).isEqualTo(0.78);
            assertThat(found.quantity().amount()).isEqualByComparingTo("200");
            assertThat(found.quantity().unit()).isEqualTo("g");
            assertThat(found.createdAt()).isNotNull();
        });
    }

    @Test
    void findsByDateOnlyAndOrders() {
        LocalDate day = LocalDate.of(2026, 7, 1);
        adapter.save(sampleLog(day, MealType.BREAKFAST));
        adapter.save(sampleLog(day, MealType.DINNER));

        List<FoodLog> onDate = adapter.findByDate(day);
        List<FoodLog> otherDay = adapter.findByDate(day.plusDays(1));

        assertThat(onDate).hasSize(2);
        assertThat(onDate).allSatisfy(log -> assertThat(log.date()).isEqualTo(day));
        assertThat(otherDay).isEmpty();
    }

    @Test
    void deleteReturnsTrueThenFalse() {
        FoodLog saved = adapter.save(sampleLog(LocalDate.of(2026, 8, 1), MealType.SNACK));

        assertThat(adapter.deleteById(saved.id())).isTrue();
        assertThat(adapter.findById(saved.id())).isEmpty();
        assertThat(adapter.deleteById(saved.id())).isFalse();
        assertThat(adapter.deleteById(UUID.randomUUID())).isFalse();
    }

    private static FoodLog sampleLog(LocalDate date, MealType mealType) {
        NutritionEstimate estimate = new NutritionEstimate(
                "bife bovino grelhado + arroz cozido",
                FoodQuantity.of(BigDecimal.valueOf(200), "g"),
                new NutritionFacts(BigDecimal.valueOf(680), BigDecimal.valueOf(57),
                        BigDecimal.valueOf(56), BigDecimal.valueOf(23)),
                Confidence.of(0.78), EstimationSource.LOCAL_AGENT, "ok", List.of());
        return FoodLog.create(date, mealType, "2 bifes médios e 200g de arroz",
                FoodQuantity.of(BigDecimal.valueOf(200), "g"), "nota", estimate,
                Instant.parse("2026-06-12T12:00:00Z"));
    }
}
