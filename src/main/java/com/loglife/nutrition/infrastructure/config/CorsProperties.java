package com.loglife.nutrition.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS configuration, bound from {@code loglife.cors.*}. The PWA is served same-origin by this
 * app, so it needs no CORS by default — the default is therefore an <em>empty</em> list (no
 * cross-origin allowed). Set explicit origins to permit a separately hosted UI; avoid {@code *}
 * since this is an unauthenticated, write-capable personal-data API.
 */
@ConfigurationProperties(prefix = "loglife.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
