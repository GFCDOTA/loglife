package com.loglife.nutrition.api;

import com.loglife.nutrition.domain.FoodLog;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Renders food logs as RFC-4180-style CSV (fields with commas, quotes or newlines are quoted,
 * quotes doubled). Presentation concern only — no business rules.
 */
final class FoodLogCsvWriter {

    private static final String HEADER =
            "date,mealType,name,description,quantity,unit,calories,proteinGrams,carbsGrams,"
                    + "fatGrams,confidence,source,notes,createdAt";

    private FoodLogCsvWriter() {
    }

    static String write(List<FoodLog> logs) {
        return Stream.concat(
                        Stream.of(HEADER),
                        logs.stream().map(FoodLogCsvWriter::row))
                .collect(Collectors.joining("\n", "", "\n"));
    }

    private static String row(FoodLog log) {
        return Stream.of(
                        log.date(), log.mealType(), log.normalizedFoodName(),
                        log.descriptionOriginal(), log.quantity().amount(), log.quantity().unit(),
                        log.nutrition().calories(), log.nutrition().proteinGrams(),
                        log.nutrition().carbsGrams(), log.nutrition().fatGrams(),
                        log.confidence().value(), log.source(), log.notes(), log.createdAt())
                .map(FoodLogCsvWriter::field)
                .collect(Collectors.joining(","));
    }

    private static String field(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
