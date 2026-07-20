package com.loglife.nutrition.api;

import com.loglife.nutrition.api.dto.CreateFoodLogRequest;
import com.loglife.nutrition.api.dto.FoodLogResponse;
import com.loglife.nutrition.api.dto.FrequentFoodResponse;
import com.loglife.nutrition.api.dto.RepeatFoodLogRequest;
import com.loglife.nutrition.api.dto.UpdateFoodLogRequest;
import com.loglife.nutrition.application.usecase.CreateFoodLog;
import com.loglife.nutrition.application.usecase.DeleteFoodLog;
import com.loglife.nutrition.application.usecase.GetFrequentFoods;
import com.loglife.nutrition.application.usecase.ListFoodLogsByDate;
import com.loglife.nutrition.application.usecase.RepeatFoodLog;
import com.loglife.nutrition.application.usecase.UpdateFoodLog;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for food logs. Controllers only adapt HTTP &lt;-&gt; use cases; there is no
 * business logic here.
 */
@RestController
@RequestMapping("/api/v1/food-logs")
public class FoodLogController {

    private final CreateFoodLog createFoodLog;
    private final ListFoodLogsByDate listFoodLogsByDate;
    private final DeleteFoodLog deleteFoodLog;
    private final UpdateFoodLog updateFoodLog;
    private final GetFrequentFoods getFrequentFoods;
    private final RepeatFoodLog repeatFoodLog;

    public FoodLogController(CreateFoodLog createFoodLog,
                            ListFoodLogsByDate listFoodLogsByDate,
                            DeleteFoodLog deleteFoodLog,
                            UpdateFoodLog updateFoodLog,
                            GetFrequentFoods getFrequentFoods,
                            RepeatFoodLog repeatFoodLog) {
        this.createFoodLog = createFoodLog;
        this.listFoodLogsByDate = listFoodLogsByDate;
        this.deleteFoodLog = deleteFoodLog;
        this.updateFoodLog = updateFoodLog;
        this.getFrequentFoods = getFrequentFoods;
        this.repeatFoodLog = repeatFoodLog;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<FoodLogResponse> create(@Valid @RequestBody CreateFoodLogRequest request) {
        // One free-text entry may be split into several logs (one per food item).
        return createFoodLog.handle(FoodLogApiMapper.toCommand(request)).stream()
                .map(FoodLogApiMapper::toResponse)
                .toList();
    }

    @GetMapping
    public List<FoodLogResponse> listByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return listFoodLogsByDate.handle(date).stream()
                .map(FoodLogApiMapper::toResponse)
                .toList();
    }

    @GetMapping("/frequent")
    public List<FrequentFoodResponse> frequent(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "8") int limit) {
        // Clamp instead of erroring: out-of-range paging params are a UI bug, not a user mistake.
        days = Math.max(1, days);
        limit = Math.clamp(limit, 1, 50);
        return getFrequentFoods.handle(new GetFrequentFoods.Query(date, days, limit)).stream()
                .map(FoodLogApiMapper::toResponse)
                .toList();
    }

    @PostMapping("/{id}/repeat")
    @ResponseStatus(HttpStatus.CREATED)
    public FoodLogResponse repeat(@PathVariable UUID id,
                                  @Valid @RequestBody RepeatFoodLogRequest request) {
        return FoodLogApiMapper.toResponse(
                repeatFoodLog.handle(FoodLogApiMapper.toCommand(id, request)));
    }

    @PatchMapping("/{id}")
    public FoodLogResponse update(@PathVariable UUID id,
                                  @Valid @RequestBody UpdateFoodLogRequest request) {
        return FoodLogApiMapper.toResponse(
                updateFoodLog.handle(FoodLogApiMapper.toCommand(id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteFoodLog.handle(id);
    }
}
