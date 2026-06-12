package com.loglife.nutrition.domain;

import java.math.BigDecimal;

/**
 * Value object for an (optional) amount + unit, e.g. {@code 200 g} or {@code 2 "unidade média"}.
 * Both parts are optional because the user may only describe food in free text.
 */
public record FoodQuantity(BigDecimal amount, String unit) {

    public FoodQuantity {
        if (amount != null && amount.signum() < 0) {
            throw new IllegalArgumentException("quantity amount must not be negative, got: " + amount);
        }
        if (unit != null && unit.isBlank()) {
            unit = null;
        }
    }

    public static FoodQuantity none() {
        return new FoodQuantity(null, null);
    }

    public static FoodQuantity of(BigDecimal amount, String unit) {
        return new FoodQuantity(amount, unit);
    }

    public boolean isPresent() {
        return amount != null || unit != null;
    }
}
