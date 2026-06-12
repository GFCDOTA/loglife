package com.loglife.nutrition.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enables CORS for the API so the PWA can be served from another origin (e.g. a phone hitting
 * {@code http://<machine-ip>:8080}). Origins are configurable via {@code loglife.cors.allowed-origins}.
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public WebCorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(corsProperties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
