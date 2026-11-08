package com.rajat.hotelbooking.booking.dto;

import com.rajat.hotelbooking.common.entity.enums.BookingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID variantId,
        @NotNull @FutureOrPresent LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @NotNull @Min(1) @Max(10) Integer rooms,
        @NotNull @Min(1) @Max(50) Integer guests,
        @NotNull BookingType bookingType,
        @NotNull @Valid CustomerRequest customer,
        @Valid GuestRequest guest) {

    public static final int MAX_NIGHTS = 30;

    @AssertTrue(message = "checkOut must be after checkIn")
    public boolean isCheckOutAfterCheckIn() {
        return checkIn == null || checkOut == null || checkOut.isAfter(checkIn);
    }

    @AssertTrue(message = "stay must be at most " + MAX_NIGHTS + " nights")
    public boolean isStayWithinLimit() {
        return checkIn == null || checkOut == null || ChronoUnit.DAYS.between(checkIn, checkOut) <= MAX_NIGHTS;
    }
}
