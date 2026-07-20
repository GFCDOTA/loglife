package com.loglife.nutrition.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA mapping for the single-row {@code user_goal} table. Persistence detail only; the domain
 * type is {@link com.loglife.nutrition.domain.NutritionGoal}.
 */
@Entity
@Table(name = "user_goal")
public class UserGoalJpaEntity {

    /** Always {@link #SINGLETON_ID}: the schema CHECK pins this table to one row. */
    static final short SINGLETON_ID = 1;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Short id;

    @Column(name = "calories", nullable = false)
    private BigDecimal calories;

    @Column(name = "protein_grams")
    private BigDecimal proteinGrams;

    @Column(name = "carbs_grams")
    private BigDecimal carbsGrams;

    @Column(name = "fat_grams")
    private BigDecimal fatGrams;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserGoalJpaEntity() {
        // for JPA
    }

    UserGoalJpaEntity(BigDecimal calories, BigDecimal proteinGrams, BigDecimal carbsGrams,
                      BigDecimal fatGrams, Instant updatedAt) {
        this.id = SINGLETON_ID;
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatGrams = fatGrams;
        this.updatedAt = updatedAt;
    }

    BigDecimal getCalories() {
        return calories;
    }

    BigDecimal getProteinGrams() {
        return proteinGrams;
    }

    BigDecimal getCarbsGrams() {
        return carbsGrams;
    }

    BigDecimal getFatGrams() {
        return fatGrams;
    }
}
