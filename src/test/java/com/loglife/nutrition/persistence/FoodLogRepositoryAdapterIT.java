package com.loglife.nutrition.persistence;

import com.loglife.AbstractPostgresIntegrationTest;
import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.FrequentFood;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void findsByDateOnlyAndOrdersByCreatedAtAsc() {
        LocalDate day = LocalDate.of(2026, 7, 1);
        // Saved out of creation order on purpose: DINNER was logged first (earlier createdAt).
        adapter.save(sampleLog(day, MealType.BREAKFAST, Instant.parse("2026-07-01T12:00:00Z")));
        adapter.save(sampleLog(day, MealType.DINNER, Instant.parse("2026-07-01T08:00:00Z")));

        List<FoodLog> onDate = adapter.findByDate(day);
        List<FoodLog> otherDay = adapter.findByDate(day.plusDays(1));

        assertThat(onDate).extracting(FoodLog::mealType)
                .containsExactly(MealType.DINNER, MealType.BREAKFAST);
        assertThat(onDate).allSatisfy(log -> assertThat(log.date()).isEqualTo(day));
        assertThat(otherDay).isEmpty();
    }

    @Test
    void frequentFoodsGroupByNameOrderByCountAndRespectWindow() {
        LocalDate today = LocalDate.of(2026, 7, 20);
        // "café com leite" 3x inside the window (latest on the 19th), "pão" 2x, "sopa" only
        // outside the window (>30 days ago) — must not appear.
        adapter.save(namedLog("café com leite", today.minusDays(3), Instant.parse("2026-07-17T08:00:00Z")));
        adapter.save(namedLog("café com leite", today.minusDays(2), Instant.parse("2026-07-18T08:00:00Z")));
        adapter.save(namedLog("café com leite", today.minusDays(1), Instant.parse("2026-07-19T08:00:00Z")));
        adapter.save(namedLog("pão", today.minusDays(2), Instant.parse("2026-07-18T09:00:00Z")));
        adapter.save(namedLog("pão", today.minusDays(1), Instant.parse("2026-07-19T09:00:00Z")));
        adapter.save(namedLog("sopa", today.minusDays(40), Instant.parse("2026-06-10T20:00:00Z")));

        List<FrequentFood> frequent = adapter.findFrequentSince(today.minusDays(30), 8);

        assertThat(frequent).extracting(f -> f.lastLog().normalizedFoodName())
                .containsExactly("café com leite", "pão");
        assertThat(frequent.get(0).timesLogged()).isEqualTo(3);
        // The carried log is the LATEST one of that name (its nutrition is what repeat clones).
        assertThat(frequent.get(0).lastLog().date()).isEqualTo(today.minusDays(1));
        assertThat(frequent.get(1).timesLogged()).isEqualTo(2);
    }

    @Test
    void saveAllIsAtomicWhenOneRowViolatesTheSchema() {
        LocalDate day = LocalDate.of(2026, 9, 1);
        // The domain only forbids negatives; 1e9 kcal passes it but overflows NUMERIC(10,2),
        // so the SECOND row fails at flush — the first must roll back with it.
        FoodLog ok = sampleLog(day, MealType.LUNCH);
        FoodLog overflows = FoodLog.create(day, MealType.DINNER, "log inválido",
                FoodQuantity.none(), null,
                new NutritionEstimate("estouro", FoodQuantity.none(),
                        new NutritionFacts(BigDecimal.valueOf(1_000_000_000L), BigDecimal.ONE,
                                BigDecimal.ONE, BigDecimal.ONE),
                        Confidence.of(0.5), EstimationSource.MOCK, null, List.of()),
                Instant.parse("2026-09-01T12:00:00Z"));

        assertThatThrownBy(() -> adapter.saveAll(List.of(ok, overflows)))
                .isInstanceOf(Exception.class);

        assertThat(adapter.findByDate(day)).isEmpty();
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
        return sampleLog(date, mealType, Instant.parse("2026-06-12T12:00:00Z"));
    }

    private static FoodLog namedLog(String foodName, LocalDate date, Instant createdAt) {
        NutritionEstimate estimate = new NutritionEstimate(
                foodName, FoodQuantity.none(),
                new NutritionFacts(BigDecimal.valueOf(120), BigDecimal.valueOf(6),
                        BigDecimal.valueOf(10), BigDecimal.valueOf(6)),
                Confidence.of(0.8), EstimationSource.OLLAMA, "ok", List.of());
        return FoodLog.create(date, MealType.BREAKFAST, foodName,
                FoodQuantity.none(), null, estimate, createdAt);
    }

    private static FoodLog sampleLog(LocalDate date, MealType mealType, Instant createdAt) {
        NutritionEstimate estimate = new NutritionEstimate(
                "bife bovino grelhado + arroz cozido",
                FoodQuantity.of(BigDecimal.valueOf(200), "g"),
                new NutritionFacts(BigDecimal.valueOf(680), BigDecimal.valueOf(57),
                        BigDecimal.valueOf(56), BigDecimal.valueOf(23)),
                Confidence.of(0.78), EstimationSource.LOCAL_AGENT, "ok", List.of());
        return FoodLog.create(date, mealType, "2 bifes médios e 200g de arroz",
                FoodQuantity.of(BigDecimal.valueOf(200), "g"), "nota", estimate, createdAt);
    }
}
