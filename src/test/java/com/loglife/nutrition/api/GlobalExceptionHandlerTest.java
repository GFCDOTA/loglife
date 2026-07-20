package com.loglife.nutrition.api;

import com.loglife.nutrition.api.dto.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the error-boundary contract: client mistakes are 400 with a named field; a broken domain
 * invariant (any bare IllegalArgumentException) is OUR bug and must be 500 — never disguised as a
 * client error.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/food-logs");

    @Test
    void invalidRequestFieldMapsTo400WithFieldError() {
        ResponseEntity<ApiError> response = handler.handleInvalidRequest(
                new InvalidRequestException("mealType", "Unknown mealType: 'BRUNCH'"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.fieldErrors()).singleElement().satisfies(fe -> {
            assertThat(fe.field()).isEqualTo("mealType");
            assertThat(fe.message()).contains("BRUNCH");
        });
    }

    @Test
    void bareIllegalArgumentIsNotHandledAsClientError() {
        // No @ExceptionHandler may list IllegalArgumentException explicitly: it must fall through
        // to the Exception catch-all (500). Otherwise a broken invariant hides as a 400.
        boolean claimed = Arrays.stream(GlobalExceptionHandler.class.getDeclaredMethods())
                .map(m -> m.getAnnotation(ExceptionHandler.class))
                .filter(a -> a != null)
                .flatMap(a -> Arrays.stream(a.value()))
                .anyMatch(type -> type.equals(IllegalArgumentException.class));

        assertThat(claimed)
                .as("IllegalArgumentException must not be mapped to a client error")
                .isFalse();
    }

    @Test
    void brokenInvariantMapsTo500WithoutLeakingInternals() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(
                new IllegalArgumentException("confidence must be within [0,1]"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).doesNotContain("confidence");
    }
}
