package com.loglife.nutrition.domain;

import java.math.BigDecimal;
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

    /**
     * Create a food log for a single {@link EstimatedItem} extracted from the description, so a
     * free-text entry with several foods ("bife + ovo + pão") becomes one log per food. The
     * original full text is kept as provenance; the item name becomes the normalized name.
     */
    public static FoodLog fromItem(LocalDate date,
                                   MealType mealType,
                                   String descriptionOriginal,
                                   EstimatedItem item,
                                   EstimationSource source,
                                   String explanation,
                                   String notes,
                                   Instant now) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(now, "now");
        return new FoodLog(
                UUID.randomUUID(), date, mealType, descriptionOriginal,
                item.name(), safeQuantity(item.quantity(), item.unit()), item.nutrition(), item.confidence(),
                source, notes, explanation, now, now);
    }

    /**
     * Build a {@link FoodQuantity} from a (possibly malformed) estimator item. A misbehaving
     * estimator may return a negative amount; treat that as "no amount" instead of letting the
     * {@code FoodQuantity} invariant crash the whole log creation.
     */
    private static FoodQuantity safeQuantity(BigDecimal amount, String unit) {
        BigDecimal safeAmount = (amount != null && amount.signum() >= 0) ? amount : null;
        boolean hasUnit = unit != null && !unit.isBlank();
        return (safeAmount != null || hasUnit) ? new FoodQuantity(safeAmount, unit) : FoodQuantity.none();
    }

    /**
     * Apply a user edit, returning a new instance. {@code null} means "keep the current value".
     * Editing the nutrition numbers reclassifies the log as {@link EstimationSource#USER_OVERRIDE}
     * with full confidence — the user's own correction outranks any estimator. Metadata-only edits
     * keep the original estimation provenance untouched.
     */
    public FoodLog withUserEdits(LocalDate newDate, MealType newMealType, String newDescription,
                                 FoodQuantity newQuantity, String newNotes,
                                 NutritionFacts overriddenNutrition, Instant now) {
        Objects.requireNonNull(now, "now");
        boolean overrides = overriddenNutrition != null;
        return new FoodLog(
                id,
                newDate != null ? newDate : date,
                newMealType != null ? newMealType : mealType,
                newDescription != null ? newDescription : descriptionOriginal,
                normalizedFoodName,
                (newQuantity != null && newQuantity.isPresent()) ? newQuantity : quantity,
                overrides ? overriddenNutrition : nutrition,
                overrides ? Confidence.of(1.0) : confidence,
                overrides ? EstimationSource.USER_OVERRIDE : source,
                newNotes != null ? newNotes : notes,
                explanation,
                createdAt,
                now);
    }

    /**
     * Clone this log onto a new date (repeating an everyday meal) — the persisted nutrition is
     * reused as-is, so no estimator is involved. Provenance (source, confidence) is preserved:
     * the numbers still come from wherever they originally came from.
     */
    public FoodLog repeatedOn(LocalDate newDate, MealType newMealType, Instant now) {
        Objects.requireNonNull(newDate, "newDate");
        Objects.requireNonNull(now, "now");
        return new FoodLog(
                UUID.randomUUID(), newDate,
                newMealType != null ? newMealType : mealType,
                descriptionOriginal, normalizedFoodName, quantity, nutrition,
                confidence, source, notes, explanation, now, now);
    }

    /**
     * Replace the numbers with a fresh estimate of the same description, keeping the log's
     * identity, day and notes. Used to upgrade a low-trust entry (e.g. MOCK logged while the
     * real estimator was down). A multi-item estimate is applied as its aggregate — the log
     * stays ONE row.
     */
    public FoodLog withFreshEstimate(NutritionEstimate estimate, Instant now) {
        Objects.requireNonNull(estimate, "estimate");
        Objects.requireNonNull(now, "now");
        return new FoodLog(
                id, date, mealType, descriptionOriginal,
                estimate.foodName(), quantity, estimate.nutrition(),
                estimate.confidence(), estimate.source(), notes, estimate.explanation(),
                createdAt, now);
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
