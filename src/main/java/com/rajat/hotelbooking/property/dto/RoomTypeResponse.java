package com.rajat.hotelbooking.property.dto;

import com.rajat.hotelbooking.common.dto.MoneyResponse;
import com.rajat.hotelbooking.reference.dto.AmenityResponse;

import java.util.List;
import java.util.UUID;

public record RoomTypeResponse(
        UUID id,
        String name,
        int maxOccupancy,
        MoneyResponse basePrice,
        List<AmenityResponse> baseFeatures,
        int totalRooms,
        List<VariantResponse> variants) {
}
