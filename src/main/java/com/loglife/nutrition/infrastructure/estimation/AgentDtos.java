package com.loglife.nutrition.infrastructure.estimation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Wire DTOs for the local HTTP agent contract ({@code POST /estimate-calories}). Kept in the
 * infrastructure layer so the transport shape never leaks into the domain.
 */
final class AgentDtos {

    private AgentDtos() {
    }

    record EstimateRequest(String description, String language, String date) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EstimateItem(
            String name,
            BigDecimal quantity,
            String unit,
            BigDecimal calories,
            BigDecimal proteinGrams,
            BigDecimal carbsGrams,
            BigDecimal fatGrams,
            Double confidence) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EstimateTotal(
            BigDecimal calories,
            BigDecimal proteinGrams,
            BigDecimal carbsGrams,
            BigDecimal fatGrams) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EstimateResponse(
            List<EstimateItem> items,
            EstimateTotal total,
            String source,
            Double confidence,
            String explanation) {
    }
}
