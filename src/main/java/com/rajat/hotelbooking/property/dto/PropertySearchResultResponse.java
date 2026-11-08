package com.rajat.hotelbooking.property.dto;

import com.rajat.hotelbooking.common.dto.MoneyResponse;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;

import java.util.List;
import java.util.UUID;

public record PropertySearchResultResponse(
        UUID propertyId,
        String name,
        PropertyKind kind,
        Integer starRating,
        String locality,
        String city,
        List<String> amenities,
        MoneyResponse lowestTotalPrice,
        List<OfferResponse> offers) {
}
