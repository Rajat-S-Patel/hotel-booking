package com.rajat.hotelbooking.booking.dto;

import com.rajat.hotelbooking.common.dto.MoneyResponse;

import java.time.LocalDate;

public record NightlyPriceResponse(
        LocalDate night,
        MoneyResponse baseRate,
        MoneyResponse surcharge,
        int rooms,
        MoneyResponse amount) {
}
