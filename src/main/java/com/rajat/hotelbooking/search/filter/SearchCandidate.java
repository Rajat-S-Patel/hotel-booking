package com.rajat.hotelbooking.search.filter;

import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.Variant;

import java.util.List;

public record SearchCandidate(Property property, List<Offer> offers) {

    public SearchCandidate withOffers(List<Offer> remaining) {
        return new SearchCandidate(property, remaining);
    }

    public Money lowestTotal() {
        return offers.stream()
                .map(Offer::total)
                .min((a, b) -> a.amount().compareTo(b.amount()))
                .orElseThrow();
    }

    public record Offer(Variant variant, int availableRooms, Money total, Money averageNight) {
    }
}
