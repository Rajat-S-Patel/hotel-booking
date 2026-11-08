package com.rajat.hotelbooking.booking.entity;

import com.rajat.hotelbooking.common.entity.enums.BookingType;
import com.rajat.hotelbooking.policy.entity.PolicyVersion;
import com.rajat.hotelbooking.pricing.NightlyRate;
import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.Variant;

import java.time.LocalDate;
import java.util.List;

public record BookingDraft(
        Customer customer,
        Property property,
        Variant variant,
        PolicyVersion policyVersion,
        BookingType bookingType,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int roomCount,
        int guestCount,
        GuestDetails guestDetails,
        List<NightlyRate> nightlyRates,
        String idempotencyKey,
        String requestFingerprint) {
}
