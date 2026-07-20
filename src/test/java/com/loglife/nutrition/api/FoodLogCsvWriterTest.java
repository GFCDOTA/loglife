package com.loglife.nutrition.api;

import com.loglife.nutrition.domain.Confidence;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodLog;
import com.loglife.nutrition.domain.FoodQuantity;
import com.loglife.nutrition.domain.MealType;
import com.loglife.nutrition.domain.NutritionFacts;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FoodLogCsvWriterTest {

    @Test
    void writesHeaderAndOneRowPerLog() {
        String csv = FoodLogCsvWriter.write(List.of(
                log("arroz", "200g de arroz", null),
                log("feijão", "1 concha de feijão", null)));

        List<String> lines = csv.lines().toList();
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).isEqualTo(
                "date,mealType,name,description,quantity,unit,calories,proteinGrams,carbsGrams,"
                        + "fatGrams,confidence,source,notes,createdAt");
        assertThat(lines.get(1)).startsWith("2026-06-12,LUNCH,arroz,200g de arroz,200,g,260");
    }

    @Test
    void escapesCommasQuotesAndNewlines() {
        String csv = FoodLogCsvWriter.write(List.of(
                log("bife, mal passado", "ele disse \"médio\"", "linha1\nlinha2")));

        String row = csv.lines().toList().get(1) + "\n" + csv.lines().toList().get(2);
        assertThat(row).contains("\"bife, mal passado\"");
        assertThat(row).contains("\"ele disse \"\"médio\"\"\"");
        assertThat(row).contains("\"linha1\nlinha2\"");
    }

    private static FoodLog log(String name, String description, String notes) {
        return FoodLog.reconstitute(
                UUID.randomUUID(), LocalDate.of(2026, 6, 12), MealType.LUNCH, description, name,
                FoodQuantity.of(BigDecimal.valueOf(200), "g"),
                new NutritionFacts(BigDecimal.valueOf(260), BigDecimal.valueOf(5),
                        BigDecimal.valueOf(56), BigDecimal.ONE),
                Confidence.of(0.8), EstimationSource.OLLAMA, notes, null,
                Instant.parse("2026-06-12T12:00:00Z"), Instant.parse("2026-06-12T12:00:00Z"));
    }
}
