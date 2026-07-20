package com.loglife.nutrition.domain;

import java.util.Objects;

/**
 * Read-model: a food the user logs often, represented by its most recent {@link FoodLog} (the
 * entry whose nutrition gets cloned on "repeat") plus how many times it appeared in the window.
 */
public record FrequentFood(FoodLog lastLog, int timesLogged) {

    public FrequentFood {
        Objects.requireNonNull(lastLog, "lastLog");
        if (timesLogged < 1) {
            throw new IllegalArgumentException("timesLogged must be at least 1");
        }
    }
}
