package com.loglife.nutrition.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that nutrition macros and confidence are normalised to the persisted precision in the
 * domain, so the value held in memory equals the value reconstituted from the database. Without
 * this, totals computed before saving (high precision) would drift from totals reloaded from the
 * {@code NUMERIC(10,2)} / {@code NUMERIC(3,2)} columns.
 */
class PrecisionNormalizationTest {

    @Test
    void nutritionFactsAreNormalisedToTwoDecimals() {
        NutritionFacts facts = new NutritionFacts(
                new BigDecimal("123.456"), new BigDecimal("5"),
                new BigDecimal("0.1"), new BigDecimal("9.999"));

        assertThat(facts.calories().scale()).isEqualTo(2);
        assertThat(facts.calories()).isEqualByComparingTo("123.46");
        assertThat(facts.proteinGrams()).isEqualByComparingTo("5.00");
        assertThat(facts.fatGrams()).isEqualByComparingTo("10.00");
    }

    @Test
    void sumStaysAtTwoDecimals() {
        NutritionFacts a = new NutritionFacts(
                new BigDecimal("100.10"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        NutritionFacts b = new NutritionFacts(
                new BigDecimal("0.20"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        NutritionFacts sum = a.plus(b);

        assertThat(sum.calories().scale()).isEqualTo(2);
        assertThat(sum.calories()).isEqualByComparingTo("100.30");
    }

    @Test
    void nutritionFactsRoundTripThroughDbScaleIsIdempotent() {
        NutritionFacts facts = new NutritionFacts(
                new BigDecimal("680.00"), new BigDecimal("57.00"),
                new BigDecimal("56.00"), new BigDecimal("23.00"));

        // Simulate the NUMERIC(10,2) write+read.
        BigDecimal persistedCalories = facts.calories().setScale(2, RoundingMode.HALF_UP);
        assertThat(persistedCalories).isEqualTo(facts.calories());
    }

    @Test
    void confidenceIsRoundedToTwoDecimals() {
        // 0.716 -> 0.72 (HALF_UP); matches the NUMERIC(3,2) column exactly.
        assertThat(Confidence.of(0.716).value()).isEqualTo(0.72);
        assertThat(Confidence.clamp(1.0 / 3.0).value()).isEqualTo(0.33);
        assertThat(Confidence.of(0.78).value()).isEqualTo(0.78);
    }

    @Test
    void confidenceRoundTripThroughDbScaleIsIdempotent() {
        Confidence original = Confidence.of(0.716); // becomes 0.72 in memory
        BigDecimal persisted = BigDecimal.valueOf(original.value()).setScale(2, RoundingMode.HALF_UP);
        Confidence reconstituted = new Confidence(persisted.doubleValue());

        assertThat(reconstituted.value()).isEqualTo(original.value());
    }
}
