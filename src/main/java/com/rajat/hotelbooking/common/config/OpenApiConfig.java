package com.rajat.hotelbooking.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hotelBookingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Hotel Booking API")
                .version("v1")
                .description("Discover properties, onboard owners, book rooms, pay and cancel. "
                        + "POST /bookings and POST /bookings/{reference}/payments require an Idempotency-Key header."));
    }
}
