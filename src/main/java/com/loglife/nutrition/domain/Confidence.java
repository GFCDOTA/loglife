package com.loglife.nutrition.domain;

/**
 * How confident an estimator is in a nutritional estimate, expressed as a value in [0, 1].
 */
public record Confidence(double value) {

    public static final Confidence ZERO = new Confidence(0.0);

    public Confidence {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0 and 1, got: " + value);
        }
        // Round to two decimals to match the NUMERIC(3,2) storage column, so the value kept in
        // memory equals the value reconstituted from the database (idempotent round-trip).
        value = Math.round(value * 100.0) / 100.0;
    }

    public static Confidence of(double value) {
        return new Confidence(value);
    }

    /** Clamp an arbitrary (possibly null/out-of-range) value into a valid Confidence. */
    public static Confidence clamp(Double value) {
        if (value == null || Double.isNaN(value)) {
            return ZERO;
        }
        return new Confidence(Math.max(0.0, Math.min(1.0, value)));
    }
}
