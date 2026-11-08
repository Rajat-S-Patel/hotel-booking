package com.rajat.hotelbooking.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "hotelbooking.booking")
public record BookingProperties(
        @DefaultValue("15m") Duration holdDuration,
        @DefaultValue("60s") Duration expiryInterval,
        @DefaultValue("50") int expiryBatchSize) {
}
