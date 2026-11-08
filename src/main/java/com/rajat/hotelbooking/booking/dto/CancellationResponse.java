package com.rajat.hotelbooking.booking.dto;

import com.rajat.hotelbooking.booking.entity.enums.BookingStatus;
import com.rajat.hotelbooking.payment.dto.RefundResponse;

import java.time.Instant;

public record CancellationResponse(
        String reference,
        BookingStatus status,
        Instant cancelledAt,
        RefundResponse refund) {
}
