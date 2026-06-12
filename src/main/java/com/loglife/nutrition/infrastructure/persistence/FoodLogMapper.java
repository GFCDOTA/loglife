package com.loglife.nutrition.infrastructure.persistence;

import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionFacts;

import java.math.BigDecimal;

/**
 * Maps between the {@link FoodLog} domain aggregate and its {@link FoodLogJpaEntity}
 * persistence representation. Package-private: only the persistence adapter uses it.
 */
final class FoodLogMapper {

    private FoodLogMapper() {
    }

    static FoodLogJpaEntity toEntity(FoodLog domain) {
        FoodLogJpaEntity entity = new FoodLogJpaEntity();
        entity.setId(domain.id());
        entity.setLogDate(domain.date());
        entity.setMealType(domain.mealType().name());
        entity.setDescriptionOriginal(domain.descriptionOriginal());
        entity.setNormalizedFoodName(domain.normalizedFoodName());
        entity.setQuantityAmount(domain.quantity().amount());
        entity.setQuantityUnit(domain.quantity().unit());
        entity.setCalories(domain.nutrition().calories());
        entity.setProteinGrams(domain.nutrition().proteinGrams());
        entity.setCarbsGrams(domain.nutrition().carbsGrams());
        entity.setFatGrams(domain.nutrition().fatGrams());
        entity.setConfidence(BigDecimal.valueOf(domain.confidence().value()));
        entity.setSource(domain.source().name());
        entity.setNotes(domain.notes());
        entity.setExplanation(domain.explanation());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    static FoodLog toDomain(FoodLogJpaEntity entity) {
        return FoodLog.reconstitute(
                entity.getId(),
                entity.getLogDate(),
                MealType.valueOf(entity.getMealType()),
                entity.getDescriptionOriginal(),
                entity.getNormalizedFoodName(),
                new FoodQuantity(entity.getQuantityAmount(), entity.getQuantityUnit()),
                new NutritionFacts(entity.getCalories(), entity.getProteinGrams(),
                        entity.getCarbsGrams(), entity.getFatGrams()),
                new Confidence(entity.getConfidence().doubleValue()),
                EstimationSource.valueOf(entity.getSource()),
                entity.getNotes(),
                entity.getExplanation(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
