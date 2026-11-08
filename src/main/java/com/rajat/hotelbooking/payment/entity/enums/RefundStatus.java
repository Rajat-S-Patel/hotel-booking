package com.rajat.hotelbooking.payment.entity.enums;

public enum RefundStatus {
    INITIATED,
    SUCCEEDED,
    FAILED;

    public boolean canTransitionTo(RefundStatus next) {
        return this == INITIATED && (next == SUCCEEDED || next == FAILED);
    }
}
