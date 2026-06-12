package com.loglife.nutrition.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Structured error payload returned for every handled error, so clients never receive a raw
 * stack trace or an opaque 500.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }
}
