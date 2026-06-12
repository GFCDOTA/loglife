package com.loglife.nutrition.domain;

/**
 * The kind of meal a {@link FoodLog} belongs to.
 */
public enum MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    OTHER;

    /**
     * Parse a meal type from user/API input, case-insensitively.
     *
     * @throws IllegalArgumentException if the value is null or not a known meal type
     */
    public static MealType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("mealType must not be blank");
        }
        try {
            return MealType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown mealType: '" + value + "'");
        }
    }
}
