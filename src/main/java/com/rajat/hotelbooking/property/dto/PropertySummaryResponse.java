package com.rajat.hotelbooking.property.dto;

import com.rajat.hotelbooking.property.entity.enums.PropertyKind;

import java.util.UUID;

public record PropertySummaryResponse(
        UUID id,
        String name,
        PropertyKind kind,
        Integer starRating,
        String locality,
        String city,
        boolean active) {
}
