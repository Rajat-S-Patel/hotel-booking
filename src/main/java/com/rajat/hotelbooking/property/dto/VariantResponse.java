package com.rajat.hotelbooking.property.dto;

import com.rajat.hotelbooking.common.dto.MoneyResponse;
import com.rajat.hotelbooking.reference.dto.AmenityResponse;

import java.util.List;
import java.util.UUID;

public record VariantResponse(
        UUID id,
        String name,
        boolean standard,
        MoneyResponse surcharge,
        MoneyResponse pricePerNight,
        int roomCount,
        List<AmenityResponse> features,
        boolean active) {
}
