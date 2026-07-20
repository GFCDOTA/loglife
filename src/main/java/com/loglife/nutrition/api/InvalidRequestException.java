package com.loglife.nutrition.api;

/**
 * A client sent a syntactically valid body with a semantically invalid field (e.g. an unknown
 * {@code mealType}). Raised only at the API boundary — domain invariant violations are NOT this:
 * they are bugs and must surface as 500, not be blamed on the client.
 */
public class InvalidRequestException extends RuntimeException {

    private final String field;

    public InvalidRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
