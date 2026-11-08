package com.rajat.hotelbooking.property.dto;

import com.rajat.hotelbooking.common.dto.MoneyResponse;

import java.util.List;
import java.util.UUID;

public record OfferResponse(
        UUID variantId,
        String roomType,
        String variant,
        int maxOccupancy,
        int availableRooms,
        MoneyResponse averagePricePerNight,
        MoneyResponse totalPrice,
        List<String> features) {
}
