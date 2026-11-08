package com.rajat.hotelbooking.common.config;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ValidationConfig {

    @Bean
    public ValidationConfigurationCustomizer clockAwareValidation(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }
}
