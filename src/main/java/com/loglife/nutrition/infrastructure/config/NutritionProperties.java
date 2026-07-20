package com.loglife.nutrition.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * General nutrition-module settings, bound from {@code loglife.nutrition.*} (estimation has its
 * own nested properties class).
 */
@ConfigurationProperties(prefix = "loglife.nutrition")
public class NutritionProperties {

    /** IANA timezone used to resolve "today" when a client omits the date (voice/shortcut). */
    private String timezone = "America/Sao_Paulo";

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
