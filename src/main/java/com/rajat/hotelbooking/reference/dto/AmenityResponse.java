package com.rajat.hotelbooking.reference.dto;

import com.rajat.hotelbooking.reference.entity.Amenity;
import com.rajat.hotelbooking.reference.entity.enums.AmenityScope;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public record AmenityResponse(String code, String displayName, AmenityScope scope) {

    public static AmenityResponse from(Amenity amenity) {
        return new AmenityResponse(amenity.getCode(), amenity.getDisplayName(), amenity.getScope());
    }

    public static List<AmenityResponse> sortedFrom(Collection<Amenity> amenities) {
        return amenities.stream()
                .sorted(Comparator.comparing(Amenity::getCode))
                .map(AmenityResponse::from)
                .toList();
    }
}
