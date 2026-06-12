package com.loglife.nutrition.domain;

import java.util.UUID;

/**
 * Raised when an operation targets a {@link FoodLog} that does not exist.
 */
public class FoodLogNotFoundException extends RuntimeException {

    private final transient UUID foodLogId;

    public FoodLogNotFoundException(UUID foodLogId) {
        super("FoodLog not found: " + foodLogId);
        this.foodLogId = foodLogId;
    }

    public UUID foodLogId() {
        return foodLogId;
    }
}
