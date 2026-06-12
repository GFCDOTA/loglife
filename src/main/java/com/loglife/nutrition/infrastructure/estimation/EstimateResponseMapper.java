package com.loglife.nutrition.infrastructure.estimation;

import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.EstimatedItem;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.NutritionEstimate;
import com.loglife.nutrition.domain.NutritionFacts;

import java.util.List;

/**
 * Converts the shared agent wire response ({@link AgentDtos.EstimateResponse}) into a domain
 * {@link NutritionEstimate}. Used by every adapter that speaks the {@code /estimate-calories}
 * JSON shape (the custom HTTP local agent and the Ollama LLM adapter).
 */
final class EstimateResponseMapper {

    private EstimateResponseMapper() {
    }

    /**
     * @return a domain estimate, or {@code null} if the response carries no usable data (no
     *         items and no totals) so the calling adapter can report a failure.
     */
    static NutritionEstimate toDomain(AgentDtos.EstimateResponse response, EstimationSource source) {
        if (response == null) {
            return null;
        }
        List<AgentDtos.EstimateItem> rawItems = response.items() == null ? List.of() : response.items();
        boolean hasTotal = response.total() != null;
        if (rawItems.isEmpty() && !hasTotal) {
            return null;
        }

        List<EstimatedItem> items = rawItems.stream()
                .map(EstimateResponseMapper::toItem)
                .toList();

        NutritionFacts total = hasTotal
                ? new NutritionFacts(
                        response.total().calories(),
                        response.total().proteinGrams(),
                        response.total().carbsGrams(),
                        response.total().fatGrams())
                : items.stream().map(EstimatedItem::nutrition)
                        .reduce(NutritionFacts.zero(), NutritionFacts::plus);

        Confidence confidence = response.confidence() != null
                ? Confidence.clamp(response.confidence())
                : averageConfidence(items);

        String foodName = items.isEmpty()
                ? "(sem nome)"
                : String.join(" + ", items.stream().map(EstimatedItem::name).toList());

        return new NutritionEstimate(
                foodName, FoodQuantity.none(), total, confidence,
                source, response.explanation(), items);
    }

    private static EstimatedItem toItem(AgentDtos.EstimateItem item) {
        NutritionFacts facts = new NutritionFacts(
                item.calories(), item.proteinGrams(), item.carbsGrams(), item.fatGrams());
        return new EstimatedItem(
                item.name() == null || item.name().isBlank() ? "(item)" : item.name(),
                item.quantity(),
                item.unit(),
                facts,
                Confidence.clamp(item.confidence()));
    }

    private static Confidence averageConfidence(List<EstimatedItem> items) {
        if (items.isEmpty()) {
            return Confidence.ZERO;
        }
        double avg = items.stream().mapToDouble(i -> i.confidence().value()).average().orElse(0.0);
        return Confidence.clamp(avg);
    }
}
