package com.rajat.hotelbooking.search.filter;

import com.rajat.hotelbooking.search.dto.PropertySearchRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// min/max price are per room per night, like the price shown on a listing
@Order(3)
@Component
public class PriceRangeFilter implements SearchFilter {

    @Override
    public SearchCandidate apply(SearchCandidate candidate, PropertySearchRequest request) {
        if (request.minPrice() == null && request.maxPrice() == null) {
            return candidate;
        }
        List<SearchCandidate.Offer> inRange = candidate.offers().stream()
                .filter(offer -> within(offer.averageNight().amount(), request.minPrice(), request.maxPrice()))
                .toList();
        return candidate.withOffers(inRange);
    }

    private static boolean within(BigDecimal price, BigDecimal min, BigDecimal max) {
        return (min == null || price.compareTo(min) >= 0) && (max == null || price.compareTo(max) <= 0);
    }
}
