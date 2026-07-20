package com.loglife.nutrition.infrastructure.config;

import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.FoodLogRepository;
import com.loglife.nutrition.application.usecase.CreateFoodLog;
import com.loglife.nutrition.application.usecase.DeleteFoodLog;
import com.loglife.nutrition.application.usecase.GetDailyNutritionSummary;
import com.loglife.nutrition.application.usecase.ListFoodLogsByDate;
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
    GetDailyNutritionSummary getDailyNutritionSummary(FoodLogRepository repository) {
        return new GetDailyNutritionSummary(repository);
    }

    @Bean
    DeleteFoodLog deleteFoodLog(FoodLogRepository repository) {
        return new DeleteFoodLog(repository);
    }

    @Bean
    UpdateFoodLog updateFoodLog(FoodLogRepository repository, Clock clock) {
        return new UpdateFoodLog(repository, clock);
    }
}
