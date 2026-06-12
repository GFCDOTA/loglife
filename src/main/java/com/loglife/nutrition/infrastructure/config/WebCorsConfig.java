package com.loglife.nutrition.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Enables CORS for the API only when explicit origins are configured via
 * {@code loglife.cors.allowed-origins}. The bundled PWA is same-origin and needs no CORS, so by
 * default no cross-origin access is granted (safer for an unauthenticated, write-capable API).
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public WebCorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = corsProperties.getAllowedOrigins().stream()
                .filter(o -> o != null && !o.isBlank())
                .toList();
        if (origins.isEmpty()) {
            return; // same-origin PWA needs no CORS; opt in by configuring explicit origins
        }
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
