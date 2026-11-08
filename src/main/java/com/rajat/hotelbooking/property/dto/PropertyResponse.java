package com.rajat.hotelbooking.property.dto;

import com.rajat.hotelbooking.common.entity.enums.BookingType;
import com.rajat.hotelbooking.policy.dto.PolicyResponse;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import com.rajat.hotelbooking.reference.dto.AmenityResponse;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        UUID ownerId,
        String name,
        PropertyKind kind,
        Integer starRating,
        AddressResponse address,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        String timeZone,
        Set<BookingType> acceptedBookingTypes,
        List<AmenityResponse> amenities,
        PolicyResponse cancellationPolicy,
        boolean active,
        List<RoomTypeResponse> roomTypes) {
}
