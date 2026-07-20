package com.loglife.nutrition.domain;

/**
 * Where a nutritional estimate came from. This is recorded on every {@link FoodLog} so the
 * provenance of the numbers is always explicit and auditable.
 */
public enum EstimationSource {

    /** A custom local HTTP agent (the {@code POST /estimate-calories} contract). */
    LOCAL_AGENT,

    /** A local Ollama LLM running on the user's machine. */
    OLLAMA,

    /** A deterministic, intentionally rough placeholder. NOT real nutritional data. */
    MOCK,

    /** Values entered directly by the user, no estimation involved. */
    MANUAL,

    /** An estimated log whose nutrition the user later corrected by hand (full confidence). */
    USER_OVERRIDE
}
