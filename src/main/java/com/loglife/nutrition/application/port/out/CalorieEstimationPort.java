package com.loglife.nutrition.application.port.out;

import com.loglife.nutrition.domain.FoodDescription;

/**
 * Outbound port for estimating nutrition from a free-text {@link FoodDescription}. Concrete
 * adapters consult the user's local agents (a custom HTTP agent, a local Ollama LLM, ...) or a
 * controlled mock. Implementations must NOT throw for the expected "agent is unavailable" case;
 * they return {@link EstimationResult#failure} instead, so callers can fall back cleanly.
 */
public interface CalorieEstimationPort {

    EstimationResult estimate(FoodDescription description);
}
