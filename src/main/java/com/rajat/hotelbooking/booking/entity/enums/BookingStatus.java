package com.rajat.hotelbooking.booking.entity.enums;

import java.util.EnumSet;
import java.util.Set;

public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    EXPIRED;

    public Set<BookingStatus> allowedNext() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(CONFIRMED, CANCELLED, EXPIRED);
            case CONFIRMED -> EnumSet.of(CANCELLED);
            case CANCELLED, EXPIRED -> EnumSet.noneOf(BookingStatus.class);
        };
    }
    public boolean canTransitionTo(BookingStatus next) {
        return allowedNext().contains(next);
    }
    public boolean isTerminal() {
        return allowedNext().isEmpty();
    }
    public boolean holdsInventory() {
        return this == PENDING_PAYMENT || this == CONFIRMED;
    }
}
