package com.loglife.nutrition.infrastructure.config;

import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.application.port.out.NutritionGoalRepository;
import com.loglife.nutrition.application.usecase.CreateFoodLog;
import com.loglife.nutrition.application.usecase.DeleteFoodLog;
import com.loglife.nutrition.application.usecase.GetDailyNutritionSummary;
import com.loglife.nutrition.application.usecase.GetFrequentFoods;
import com.loglife.nutrition.application.usecase.GetNutritionGoal;
import com.loglife.nutrition.application.usecase.GetNutritionTrend;
import com.loglife.nutrition.application.usecase.ListFoodLogsByDate;
import com.loglife.nutrition.application.usecase.ListFoodLogsByPeriod;
import com.loglife.nutrition.application.usecase.ReestimateFoodLog;
import com.loglife.nutrition.application.usecase.RepeatFoodLog;
import com.loglife.nutrition.application.usecase.SetNutritionGoal;
import com.loglife.nutrition.application.usecase.UpdateFoodLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the framework-agnostic use cases as Spring beans. The use cases and the domain stay
 * free of any Spring annotation; all wiring lives here in the infrastructure layer.
 */
@Configuration
public class UseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateFoodLog createFoodLog(CalorieEstimationPort estimationPort,
                                FoodLogRepository repository,
                                Clock clock) {
        return new CreateFoodLog(estimationPort, repository, clock);
    }

    @Bean
    ListFoodLogsByDate listFoodLogsByDate(FoodLogRepository repository) {
        return new ListFoodLogsByDate(repository);
    }

    @Bean
    GetDailyNutritionSummary getDailyNutritionSummary(FoodLogRepository repository,
                                                      NutritionGoalRepository goalRepository) {
        return new GetDailyNutritionSummary(repository, goalRepository);
    }

    @Bean
    GetNutritionGoal getNutritionGoal(NutritionGoalRepository goalRepository) {
        return new GetNutritionGoal(goalRepository);
    }

    @Bean
    SetNutritionGoal setNutritionGoal(NutritionGoalRepository goalRepository) {
        return new SetNutritionGoal(goalRepository);
    }

    @Bean
    DeleteFoodLog deleteFoodLog(FoodLogRepository repository) {
        return new DeleteFoodLog(repository);
    }

    @Bean
    UpdateFoodLog updateFoodLog(FoodLogRepository repository, Clock clock) {
        return new UpdateFoodLog(repository, clock);
    }

    @Bean
    GetFrequentFoods getFrequentFoods(FoodLogRepository repository) {
        return new GetFrequentFoods(repository);
    }

    @Bean
    RepeatFoodLog repeatFoodLog(FoodLogRepository repository, Clock clock) {
        return new RepeatFoodLog(repository, clock);
    }

    @Bean
    GetNutritionTrend getNutritionTrend(FoodLogRepository repository) {
        return new GetNutritionTrend(repository);
    }

    @Bean
    ListFoodLogsByPeriod listFoodLogsByPeriod(FoodLogRepository repository) {
        return new ListFoodLogsByPeriod(repository);
    }

    @Bean
    ReestimateFoodLog reestimateFoodLog(FoodLogRepository repository,
                                        CalorieEstimationPort estimationPort,
                                        Clock clock) {
        return new ReestimateFoodLog(repository, estimationPort, clock);
    }
}
