package com.rajat.hotelbooking.booking.dto;

import com.rajat.hotelbooking.booking.entity.enums.BookingStatus;
import com.rajat.hotelbooking.common.dto.MoneyResponse;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;
import com.rajat.hotelbooking.payment.entity.enums.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        String bookingReference,
        BookingStatus bookingStatus,
        PaymentStatus status,
        PaymentMethod method,
        MoneyResponse amount,
        String paymentMethodSummary,
        String gatewayReference,
        String failureReason,
        Instant completedAt) {
}
