package com.loglife.nutrition.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Minimal API protection for a single-user app, bound from {@code loglife.security.*}. No Spring
 * Security: a static token checked by a servlet filter is the right size here (house rule:
 * roadmap #11). Empty token = filter disabled — safe because the server binds loopback-only by
 * default.
 */
@ConfigurationProperties(prefix = "loglife.security")
public class SecurityProperties {

    /** Static token required in the {@code X-Api-Token} header on every /api/** call when set. */
    private String apiToken = "";

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public boolean isTokenEnabled() {
        return apiToken != null && !apiToken.isBlank();
    }
}
