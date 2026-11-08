package com.rajat.hotelbooking.search.filter;

import com.rajat.hotelbooking.search.dto.PropertySearchRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Component
public class AvailabilityFilter implements SearchFilter {

    @Override
    public SearchCandidate apply(SearchCandidate candidate, PropertySearchRequest request) {
        return candidate.withOffers(candidate.offers().stream()
                .filter(offer -> offer.availableRooms() >= request.rooms())
                .filter(offer -> offer.variant().getRoomType().getMaxOccupancy() * request.rooms() >= request.guests())
                .toList());
    }
}
