package com.rajat.hotelbooking.search.filter;

import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.RoomType;
import com.rajat.hotelbooking.property.entity.Variant;
import com.rajat.hotelbooking.reference.entity.enums.AmenityScope;
import com.rajat.hotelbooking.search.dto.PropertySearchRequest;
import com.rajat.hotelbooking.search.filter.SearchCandidate.Offer;
import com.rajat.hotelbooking.support.TestData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static com.rajat.hotelbooking.support.TestData.amenity;
import static com.rajat.hotelbooking.support.TestData.inr;
import static org.assertj.core.api.Assertions.assertThat;

class SearchFiltersTest {

    private final Property property = TestData.property();
    private final RoomType deluxe = TestData.roomType(property, 4);
    private final Variant standard = deluxe.getVariants().getFirst();
    private final Variant seaView = deluxe.addVariant("Sea View", inr("1000"), 2);

    @Test
    void availabilityDropsOffersWithoutEnoughRoomsOrBeds() {
        SearchCandidate candidate = candidate(offer(standard, 3, "3000"), offer(seaView, 1, "4000"));

        assertThat(new AvailabilityFilter().apply(candidate, request(2, 4, null, null, null)).offers())
                .extracting(Offer::variant).containsExactly(standard);
        assertThat(new AvailabilityFilter().apply(candidate, request(2, 5, null, null, null)).offers()).isEmpty();
    }

    @Test
    void propertyAmenityMustBeOnTheProperty() {
        property.addAmenity(amenity("POOL", AmenityScope.PROPERTY));
        SearchCandidate candidate = candidate(offer(standard, 3, "3000"));

        assertThat(new AmenityFilter().apply(candidate, request(1, 2, null, null, Set.of("pool"))).offers()).hasSize(1);
        assertThat(new AmenityFilter().apply(candidate, request(1, 2, null, null, Set.of("SPA"))).offers()).isEmpty();
    }

    @Test
    void roomFeatureKeepsOnlyOffersThatHaveIt() {
        seaView.addExtraFeature(amenity("SEA_VIEW", AmenityScope.ROOM));
        SearchCandidate candidate = candidate(offer(standard, 3, "3000"), offer(seaView, 2, "4000"));

        assertThat(new AmenityFilter().apply(candidate, request(1, 2, null, null, Set.of("SEA_VIEW"))).offers())
                .extracting(Offer::variant).containsExactly(seaView);
    }

    @Test
    void priceRangeIsPerNightAndInclusive() {
        SearchCandidate candidate = candidate(offer(standard, 3, "3000"), offer(seaView, 2, "4000"));

        assertThat(new PriceRangeFilter().apply(candidate, request(1, 2, "3000", "3999.99", null)).offers())
                .extracting(Offer::variant).containsExactly(standard);
        assertThat(new PriceRangeFilter().apply(candidate, request(1, 2, "3000.01", "4000", null)).offers())
                .extracting(Offer::variant).containsExactly(seaView);
    }

    private SearchCandidate candidate(Offer... offers) {
        return new SearchCandidate(property, List.of(offers));
    }

    private static Offer offer(Variant variant, int available, String perNight) {
        return new Offer(variant, available, inr(perNight).times(2), inr(perNight));
    }

    private static PropertySearchRequest request(int rooms, int guests, String minPrice, String maxPrice,
                                                 Set<String> amenities) {
        return new PropertySearchRequest("Ahmedabad", null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3),
                guests, rooms, minPrice == null ? null : new BigDecimal(minPrice),
                maxPrice == null ? null : new BigDecimal(maxPrice), amenities, null, null, null, null, null);
    }
}
