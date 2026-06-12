package com.loglife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * LogLife application entry point.
 *
 * <p>LogLife is a personal life-logging app. This service exposes the nutrition / calorie
 * module. Everything runs locally; nutritional estimates come from local agents on the
 * user's machine (see the {@code com.loglife.nutrition.infrastructure.estimation} package),
 * never from external public APIs.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LogLifeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogLifeApplication.class, args);
    }
}
