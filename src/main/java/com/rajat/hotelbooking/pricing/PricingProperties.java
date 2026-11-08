package com.rajat.hotelbooking.pricing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "hotelbooking.pricing")
public record PricingProperties(@DefaultValue("0") int weekendSurchargePercent) {

    public PricingProperties {
        if (weekendSurchargePercent < 0 || weekendSurchargePercent > 100) {
            throw new IllegalArgumentException(
                    "weekend surcharge percent must be between 0 and 100: " + weekendSurchargePercent);
        }
    }
}
