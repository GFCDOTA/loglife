package com.loglife.nutrition.api;

import com.loglife.nutrition.api.dto.DailyNutritionSummaryResponse;
import com.loglife.nutrition.api.dto.NutritionGoalRequest;
import com.loglife.nutrition.api.dto.NutritionGoalResponse;
import com.loglife.nutrition.api.dto.NutritionTrendResponse;
import com.loglife.nutrition.application.usecase.GetDailyNutritionSummary;
import com.loglife.nutrition.application.usecase.GetNutritionGoal;
import com.loglife.nutrition.application.usecase.GetNutritionTrend;
import com.loglife.nutrition.application.usecase.SetNutritionGoal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST endpoints for daily nutrition totals and the daily goal.
 */
@RestController
@RequestMapping("/api/v1/nutrition")
public class NutritionSummaryController {

    private final GetDailyNutritionSummary getDailyNutritionSummary;
    private final GetNutritionGoal getNutritionGoal;
    private final SetNutritionGoal setNutritionGoal;
    private final GetNutritionTrend getNutritionTrend;

    public NutritionSummaryController(GetDailyNutritionSummary getDailyNutritionSummary,
                                      GetNutritionGoal getNutritionGoal,
                                      SetNutritionGoal setNutritionGoal,
                                      GetNutritionTrend getNutritionTrend) {
        this.getDailyNutritionSummary = getDailyNutritionSummary;
        this.getNutritionGoal = getNutritionGoal;
        this.setNutritionGoal = setNutritionGoal;
        this.getNutritionTrend = getNutritionTrend;
    }

    @GetMapping("/daily-summary")
    public DailyNutritionSummaryResponse dailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return FoodLogApiMapper.toResponse(getDailyNutritionSummary.handle(date));
    }

    @GetMapping("/trend")
    public NutritionTrendResponse trend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "7") int days) {
        days = Math.clamp(days, 1, 90);
        return FoodLogApiMapper.toResponse(
                getNutritionTrend.handle(new GetNutritionTrend.Query(date, days)));
    }

    @GetMapping("/goal")
    public ResponseEntity<NutritionGoalResponse> goal() {
        // 204 (not 404) when unset: "no goal yet" is a normal state, not a missing resource.
        return getNutritionGoal.handle()
                .map(goal -> ResponseEntity.ok(FoodLogApiMapper.toResponse(goal)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/goal")
    public NutritionGoalResponse setGoal(@Valid @RequestBody NutritionGoalRequest request) {
        return FoodLogApiMapper.toResponse(setNutritionGoal.handle(FoodLogApiMapper.toDomain(request)));
    }
}
