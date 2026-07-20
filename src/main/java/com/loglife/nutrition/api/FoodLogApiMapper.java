package com.loglife.nutrition.api;

import com.loglife.nutrition.api.dto.CreateFoodLogRequest;
import com.loglife.nutrition.api.dto.DailyNutritionSummaryResponse;
import com.loglife.nutrition.api.dto.FoodLogResponse;
import com.loglife.nutrition.api.dto.NutritionGoalRequest;
import com.loglife.nutrition.api.dto.NutritionGoalResponse;
import com.loglife.nutrition.api.dto.NutritionValuesRequest;
import com.loglife.nutrition.api.dto.UpdateFoodLogRequest;
import com.loglife.nutrition.application.usecase.CreateFoodLog;
import com.loglife.nutrition.application.usecase.UpdateFoodLog;
import com.loglife.nutrition.domain.DailyNutritionSummary;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionFacts;
import com.loglife.nutrition.domain.NutritionGoal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps between API DTOs and the application/domain types. Pure, stateless conversion — no
 * business rules live here.
 */
final class FoodLogApiMapper {

    private FoodLogApiMapper() {
    }

    static CreateFoodLog.Command toCommand(CreateFoodLogRequest request) {
        MealType mealType;
        try {
            mealType = MealType.fromString(request.mealType());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("mealType", ex.getMessage());
        }
        FoodQuantity quantity = (request.quantity() != null || request.unit() != null)
                ? FoodQuantity.of(request.quantity(), request.unit())
                : FoodQuantity.none();
        return new CreateFoodLog.Command(
                request.date(), mealType, request.description(),
                quantity, request.notes(), request.language(), toFacts(request.nutrition()));
    }

    private static NutritionFacts toFacts(NutritionValuesRequest values) {
        return values == null ? null : new NutritionFacts(
                values.calories(), values.proteinGrams(), values.carbsGrams(), values.fatGrams());
    }

    static UpdateFoodLog.Command toCommand(UUID id, UpdateFoodLogRequest request) {
        MealType mealType = null;
        if (request.mealType() != null) {
            try {
                mealType = MealType.fromString(request.mealType());
            } catch (IllegalArgumentException ex) {
                throw new InvalidRequestException("mealType", ex.getMessage());
            }
        }
        FoodQuantity quantity = (request.quantity() != null || request.unit() != null)
                ? FoodQuantity.of(request.quantity(), request.unit())
                : null;
        return new UpdateFoodLog.Command(
                id, request.date(), mealType, request.description(), quantity,
                request.notes(), toFacts(request.nutrition()));
    }

    static FoodLogResponse toResponse(FoodLog log) {
        NutritionFacts n = log.nutrition();
        return new FoodLogResponse(
                log.id(),
                log.date(),
                log.mealType().name(),
                log.descriptionOriginal(),
                log.normalizedFoodName(),
                log.quantity().amount(),
                log.quantity().unit(),
                n.calories(),
                n.proteinGrams(),
                n.carbsGrams(),
                n.fatGrams(),
                log.confidence().value(),
                log.source().name(),
                log.notes(),
                log.explanation(),
                log.createdAt(),
                log.updatedAt());
    }

    static DailyNutritionSummaryResponse toResponse(DailyNutritionSummary summary) {
        NutritionFacts t = summary.totals();
        Map<String, DailyNutritionSummaryResponse.MealTypeBucket> byMeal = new LinkedHashMap<>();
        summary.logsByMealType().forEach((mealType, bucket) -> {
            NutritionFacts bn = bucket.totals();
            byMeal.put(mealType.name(), new DailyNutritionSummaryResponse.MealTypeBucket(
                    bucket.count(), bn.calories(), bn.proteinGrams(), bn.carbsGrams(), bn.fatGrams()));
        });
        DailyNutritionSummaryResponse.GoalBlock goal = null;
        if (summary.goalProgress() != null) {
            DailyNutritionSummary.GoalProgress p = summary.goalProgress();
            goal = new DailyNutritionSummaryResponse.GoalBlock(
                    p.goal().calories(), p.goal().proteinGrams(),
                    p.goal().carbsGrams(), p.goal().fatGrams(),
                    p.remainingCalories(), p.percentOfCalories());
        }
        return new DailyNutritionSummaryResponse(
                summary.date(),
                t.calories(), t.proteinGrams(), t.carbsGrams(), t.fatGrams(),
                summary.totalLogs(),
                byMeal,
                goal);
    }

    static NutritionGoal toDomain(NutritionGoalRequest request) {
        return new NutritionGoal(request.calories(), request.proteinGrams(),
                request.carbsGrams(), request.fatGrams());
    }

    static NutritionGoalResponse toResponse(NutritionGoal goal) {
        return new NutritionGoalResponse(goal.calories(), goal.proteinGrams(),
                goal.carbsGrams(), goal.fatGrams());
    }
}
