package com.loglife.nutrition.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root: one recorded food/meal entry for a given day. Pure domain object with no
 * dependency on Spring, JPA or any transport. Construction goes through {@link #create} (for
 * new entries) or {@link #reconstitute} (when loading from persistence); the all-args
 * constructor is private so invariants are always enforced.
 */
public final class FoodLog {

    private final UUID id;
    private final LocalDate date;
    private final MealType mealType;
    private final String descriptionOriginal;
    private final String normalizedFoodName;
    private final FoodQuantity quantity;
    private final NutritionFacts nutrition;
    private final Confidence confidence;
    private final EstimationSource source;
    private final String notes;
    private final String explanation;
    private final Instant createdAt;
    private final Instant updatedAt;

    private FoodLog(UUID id, LocalDate date, MealType mealType, String descriptionOriginal,
                    String normalizedFoodName, FoodQuantity quantity, NutritionFacts nutrition,
                    Confidence confidence, EstimationSource source, String notes, String explanation,
                    Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.date = Objects.requireNonNull(date, "date");
        this.mealType = Objects.requireNonNull(mealType, "mealType");
        if (descriptionOriginal == null || descriptionOriginal.isBlank()) {
            throw new IllegalArgumentException("descriptionOriginal must not be blank");
        }
        this.descriptionOriginal = descriptionOriginal.trim();
        this.normalizedFoodName = normalizedFoodName;
        this.quantity = quantity == null ? FoodQuantity.none() : quantity;
        this.nutrition = nutrition == null ? NutritionFacts.zero() : nutrition;
        this.confidence = confidence == null ? Confidence.ZERO : confidence;
        this.source = Objects.requireNonNull(source, "source");
        this.notes = notes;
        this.explanation = explanation;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /**
     * Create a brand-new food log from the user's input and an estimate produced by a
     * {@code CalorieEstimationPort}. A user-supplied quantity takes precedence over the
     * estimate's aggregate quantity.
     */
    public static FoodLog create(LocalDate date,
                                 MealType mealType,
                                 String descriptionOriginal,
                                 FoodQuantity userQuantity,
                                 String notes,
                                 NutritionEstimate estimate,
                                 Instant now) {
        Objects.requireNonNull(estimate, "estimate");
        Objects.requireNonNull(now, "now");
        FoodQuantity quantity = (userQuantity != null && userQuantity.isPresent())
                ? userQuantity
                : estimate.quantity();
        return new FoodLog(
                UUID.randomUUID(), date, mealType, descriptionOriginal,
                estimate.foodName(), quantity, estimate.nutrition(),
                estimate.confidence(), estimate.source(), notes, estimate.explanation(),
                now, now);
    }

    /** Reconstitute an existing food log loaded from a persistence adapter. */
    public static FoodLog reconstitute(UUID id, LocalDate date, MealType mealType, String descriptionOriginal,
                                       String normalizedFoodName, FoodQuantity quantity, NutritionFacts nutrition,
                                       Confidence confidence, EstimationSource source, String notes,
                                       String explanation, Instant createdAt, Instant updatedAt) {
        return new FoodLog(id, date, mealType, descriptionOriginal, normalizedFoodName, quantity, nutrition,
                confidence, source, notes, explanation, createdAt, updatedAt);
    }

    public UUID id() {
        return id;
    }

    public LocalDate date() {
        return date;
    }

    public MealType mealType() {
        return mealType;
    }

    public String descriptionOriginal() {
        return descriptionOriginal;
    }

    public String normalizedFoodName() {
        return normalizedFoodName;
    }

    public FoodQuantity quantity() {
        return quantity;
    }

    public NutritionFacts nutrition() {
        return nutrition;
    }

    public Confidence confidence() {
        return confidence;
    }

    public EstimationSource source() {
        return source;
    }

    public String notes() {
        return notes;
    }

    public String explanation() {
        return explanation;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof FoodLog other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
