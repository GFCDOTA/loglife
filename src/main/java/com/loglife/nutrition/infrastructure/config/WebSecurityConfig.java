package com.loglife.nutrition.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the {@link ApiTokenFilter} when a token is configured. Without a token the app is
 * open — acceptable only because the default bind is loopback; the yml documents the LAN combo
 * (LOGLIFE_BIND + LOGLIFE_API_TOKEN).
 */
@Configuration
public class WebSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Bean
    FilterRegistrationBean<ApiTokenFilter> apiTokenFilter(SecurityProperties properties) {
        FilterRegistrationBean<ApiTokenFilter> registration = new FilterRegistrationBean<>();
        if (!properties.isTokenEnabled()) {
            log.info("API token auth disabled (no LOGLIFE_API_TOKEN set)");
            registration.setEnabled(false);
            // A filter instance is still required by the registration contract; it never runs.
            registration.setFilter(new ApiTokenFilter(""));
            return registration;
        }
        log.info("API token auth ENABLED for /api/**");
        registration.setFilter(new ApiTokenFilter(properties.getApiToken()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/api/*");
        return registration;
    }
}
