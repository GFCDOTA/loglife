package com.loglife.nutrition.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA mapping for the {@code food_logs} table. This is a persistence detail only: it holds no
 * business rules. Mapping to/from the {@link com.loglife.nutrition.domain.FoodLog} aggregate is
 * done by {@link FoodLogMapper}.
 */
@Entity
@Table(name = "food_logs")
public class FoodLogJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "meal_type", nullable = false, length = 20)
    private String mealType;

    @Column(name = "description_original", nullable = false, columnDefinition = "text")
    private String descriptionOriginal;

    @Column(name = "normalized_food_name", columnDefinition = "text")
    private String normalizedFoodName;

    @Column(name = "quantity_amount")
    private BigDecimal quantityAmount;

    @Column(name = "quantity_unit", length = 50)
    private String quantityUnit;

    @Column(name = "calories", nullable = false)
    private BigDecimal calories;

    @Column(name = "protein_grams", nullable = false)
    private BigDecimal proteinGrams;

    @Column(name = "carbs_grams", nullable = false)
    private BigDecimal carbsGrams;

    @Column(name = "fat_grams", nullable = false)
    private BigDecimal fatGrams;

    @Column(name = "confidence", nullable = false)
    private BigDecimal confidence;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "explanation", columnDefinition = "text")
    private String explanation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FoodLogJpaEntity() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDate logDate) {
        this.logDate = logDate;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getDescriptionOriginal() {
        return descriptionOriginal;
    }

    public void setDescriptionOriginal(String descriptionOriginal) {
        this.descriptionOriginal = descriptionOriginal;
    }

    public String getNormalizedFoodName() {
        return normalizedFoodName;
    }

    public void setNormalizedFoodName(String normalizedFoodName) {
        this.normalizedFoodName = normalizedFoodName;
    }

    public BigDecimal getQuantityAmount() {
        return quantityAmount;
    }

    public void setQuantityAmount(BigDecimal quantityAmount) {
        this.quantityAmount = quantityAmount;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
    }

    public BigDecimal getCalories() {
        return calories;
    }

    public void setCalories(BigDecimal calories) {
        this.calories = calories;
    }

    public BigDecimal getProteinGrams() {
        return proteinGrams;
    }

    public void setProteinGrams(BigDecimal proteinGrams) {
        this.proteinGrams = proteinGrams;
    }

    public BigDecimal getCarbsGrams() {
        return carbsGrams;
    }

    public void setCarbsGrams(BigDecimal carbsGrams) {
        this.carbsGrams = carbsGrams;
    }

    public BigDecimal getFatGrams() {
        return fatGrams;
    }

    public void setFatGrams(BigDecimal fatGrams) {
        this.fatGrams = fatGrams;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
