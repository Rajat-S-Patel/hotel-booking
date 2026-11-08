package com.rajat.hotelbooking.booking.dto;

import com.rajat.hotelbooking.booking.entity.enums.BookingStatus;
import com.rajat.hotelbooking.common.dto.MoneyResponse;
import com.rajat.hotelbooking.common.entity.enums.BookingType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        String reference,
        BookingStatus status,
        BookingType bookingType,
        UUID propertyId,
        String propertyName,
        UUID variantId,
        String roomType,
        String variant,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        int rooms,
        int guests,
        CustomerSummaryResponse customer,
        GuestResponse guest,
        MoneyResponse total,
        List<NightlyPriceResponse> nightlyPrices,
        String cancellationPolicy,
        int cancellationPolicyVersion,
        Instant holdExpiresAt,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant expiredAt) {
}
