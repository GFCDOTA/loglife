package com.loglife.nutrition.application;

/**
 * Raised when no estimator (not even the controlled fallback) could produce an estimate. With
 * the default configuration the mock fallback always succeeds, so this is a safety net for
 * misconfiguration rather than an expected condition.
 */
public class EstimationUnavailableException extends RuntimeException {

    public EstimationUnavailableException(String reason) {
        super("Calorie estimation unavailable: " + (reason == null ? "unknown" : reason));
    }
}
