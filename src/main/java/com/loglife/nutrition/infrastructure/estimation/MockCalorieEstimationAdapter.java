package com.loglife.nutrition.infrastructure.estimation;

import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.EstimationResult;
import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.EstimatedItem;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodDescription;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.NutritionEstimate;
import com.loglife.nutrition.domain.NutritionFacts;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlled fallback estimator for development and tests. It returns a deterministic,
 * intentionally rough placeholder marked {@link EstimationSource#MOCK} with a LOW confidence.
 *
 * <p>It NEVER pretends to be real nutritional data — the explanation makes that explicit, and
 * the confidence is deliberately low so callers and the UI can flag it.
 */
public class MockCalorieEstimationAdapter implements CalorieEstimationPort {

    private static final Confidence LOW_CONFIDENCE = Confidence.of(0.2);
    private static final String DISCLAIMER =
            "Estimativa MOCK — valores de exemplo, NÃO são dados nutricionais reais. "
            + "Configure um agente local (loglife.nutrition.estimation.provider) para estimativas reais.";

    @Override
    public EstimationResult estimate(FoodDescription description) {
        String name = trim(description.rawText());

        // Flat placeholder figures. Not derived from any real food table on purpose.
        NutritionFacts facts = new NutritionFacts(
                BigDecimal.valueOf(250),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(8));

        EstimatedItem item = new EstimatedItem(name, null, null, facts, LOW_CONFIDENCE);

        NutritionEstimate estimate = new NutritionEstimate(
                name, FoodQuantity.none(), facts, LOW_CONFIDENCE,
                EstimationSource.MOCK, DISCLAIMER, List.of(item));

        return EstimationResult.success(estimate);
    }

    private static String trim(String text) {
        return text.length() <= 80 ? text : text.substring(0, 80);
    }
}
