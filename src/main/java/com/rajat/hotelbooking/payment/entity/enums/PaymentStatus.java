package com.rajat.hotelbooking.payment.entity.enums;

public enum PaymentStatus {
    INITIATED,
    SUCCEEDED,
    FAILED;

    public boolean canTransitionTo(PaymentStatus next) {
        return this == INITIATED && (next == SUCCEEDED || next == FAILED);
    }

    public boolean isTerminal() {
        return this != INITIATED;
    }
}
