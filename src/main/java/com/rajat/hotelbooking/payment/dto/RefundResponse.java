package com.rajat.hotelbooking.payment.dto;

import com.rajat.hotelbooking.common.dto.MoneyResponse;
import com.rajat.hotelbooking.payment.entity.enums.RefundReason;
import com.rajat.hotelbooking.payment.entity.enums.RefundStatus;

import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        MoneyResponse amount,
        RefundStatus status,
        RefundReason reason,
        String gatewayRefundReference,
        String failureReason,
        Instant completedAt) {
}
