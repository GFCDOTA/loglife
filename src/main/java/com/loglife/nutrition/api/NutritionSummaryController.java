package com.loglife.nutrition.api;

import com.loglife.nutrition.api.dto.DailyNutritionSummaryResponse;
import com.loglife.nutrition.application.usecase.GetDailyNutritionSummary;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST endpoint for daily nutrition totals.
 */
@RestController
@RequestMapping("/api/v1/nutrition")
public class NutritionSummaryController {

    private final GetDailyNutritionSummary getDailyNutritionSummary;

    public NutritionSummaryController(GetDailyNutritionSummary getDailyNutritionSummary) {
        this.getDailyNutritionSummary = getDailyNutritionSummary;
    }

    @GetMapping("/daily-summary")
    public DailyNutritionSummaryResponse dailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return FoodLogApiMapper.toResponse(getDailyNutritionSummary.handle(date));
    }
}
