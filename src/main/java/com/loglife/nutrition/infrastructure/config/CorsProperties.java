package com.loglife.nutrition.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS configuration, bound from {@code loglife.cors.*}. Lets the PWA (served from a phone over
 * the LAN) call the API. Default is permissive ({@code *}) for a local-only MVP.
 */
@ConfigurationProperties(prefix = "loglife.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of("*");

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
