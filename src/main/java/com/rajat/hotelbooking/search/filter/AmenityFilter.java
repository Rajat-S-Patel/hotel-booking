package com.rajat.hotelbooking.search.filter;

import com.rajat.hotelbooking.reference.entity.Amenity;
import com.rajat.hotelbooking.search.dto.PropertySearchRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

// Amenities asked for can be property amenities (POOL) or room features (SEA_VIEW).
// Property ones must all be present on the property; room ones decide which offers stay.
@Order(2)
@Component
public class AmenityFilter implements SearchFilter {

    @Override
    public SearchCandidate apply(SearchCandidate candidate, PropertySearchRequest request) {
        if (request.amenities() == null || request.amenities().isEmpty()) {
            return candidate;
        }
        Set<String> wanted = request.amenities().stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
        wanted.removeAll(codes(candidate.property().getAmenities()));
        if (wanted.isEmpty()) {
            return candidate;
        }
        return candidate.withOffers(candidate.offers().stream()
                .filter(offer -> codes(offer.variant().allFeatures()).containsAll(wanted))
                .toList());
    }

    private static Set<String> codes(Set<Amenity> amenities) {
        return amenities.stream().map(Amenity::getCode).collect(Collectors.toSet());
    }
}
