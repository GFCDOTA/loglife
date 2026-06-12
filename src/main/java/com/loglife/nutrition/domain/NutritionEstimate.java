package com.loglife.nutrition.domain;

import java.util.List;
import java.util.Objects;

/**
 * The result of estimating the nutrition of a {@link FoodDescription}. It aggregates one or
 * more {@link EstimatedItem}s into a single total, and always carries its {@link EstimationSource}
 * and {@link Confidence} so the numbers are never presented as authoritative fact.
 */
public record NutritionEstimate(
        String foodName,
        FoodQuantity quantity,
        NutritionFacts nutrition,
        Confidence confidence,
        EstimationSource source,
        String explanation,
        List<EstimatedItem> items) {

    public NutritionEstimate {
        Objects.requireNonNull(source, "source");
        if (foodName == null || foodName.isBlank()) {
            foodName = "(sem nome)";
        }
        if (quantity == null) {
            quantity = FoodQuantity.none();
        }
        if (nutrition == null) {
            nutrition = NutritionFacts.zero();
        }
        if (confidence == null) {
            confidence = Confidence.ZERO;
        }
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * Build an aggregate estimate from line items: totals are summed, confidence is the mean
     * of the item confidences, and the food name is derived from the item names.
     */
    public static NutritionEstimate fromItems(List<EstimatedItem> items,
                                              EstimationSource source,
                                              String explanation) {
        List<EstimatedItem> safe = items == null ? List.of() : items;
        NutritionFacts total = safe.stream()
                .map(EstimatedItem::nutrition)
                .reduce(NutritionFacts.zero(), NutritionFacts::plus);
        double avgConfidence = safe.isEmpty()
                ? 0.0
                : safe.stream().mapToDouble(item -> item.confidence().value()).average().orElse(0.0);
        String foodName = safe.isEmpty()
                ? "(sem nome)"
                : String.join(" + ", safe.stream().map(EstimatedItem::name).toList());
        return new NutritionEstimate(
                foodName, FoodQuantity.none(), total,
                Confidence.clamp(avgConfidence), source, explanation, safe);
    }
}
