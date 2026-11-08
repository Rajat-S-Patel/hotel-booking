package com.rajat.hotelbooking.search.service;

import com.rajat.hotelbooking.common.dto.MoneyResponse;
import com.rajat.hotelbooking.common.dto.PageResponse;
import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.inventory.service.InventoryService;
import com.rajat.hotelbooking.pricing.NightlyRate;
import com.rajat.hotelbooking.pricing.PricingStrategy;
import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.RoomType;
import com.rajat.hotelbooking.property.entity.Variant;
import com.rajat.hotelbooking.property.repository.PropertyRepository;
import com.rajat.hotelbooking.property.repository.RoomTypeRepository;
import com.rajat.hotelbooking.reference.entity.Amenity;
import com.rajat.hotelbooking.reference.entity.City;
import com.rajat.hotelbooking.reference.entity.Locality;
import com.rajat.hotelbooking.reference.repository.CityRepository;
import com.rajat.hotelbooking.reference.repository.LocalityRepository;
import com.rajat.hotelbooking.search.dto.OfferResponse;
import com.rajat.hotelbooking.search.dto.PropertySearchRequest;
import com.rajat.hotelbooking.search.dto.PropertySearchResultResponse;
import com.rajat.hotelbooking.search.filter.SearchCandidate.Offer;
import com.rajat.hotelbooking.search.filter.SearchCandidate;
import com.rajat.hotelbooking.search.filter.SearchFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertySearchService {

    private final CityRepository cityRepository;
    private final LocalityRepository localityRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final InventoryService inventoryService;
    private final PricingStrategy pricingStrategy;
    private final List<SearchFilter> filters;

    public PageResponse<PropertySearchResultResponse> search(PropertySearchRequest request) {
        City city = cityRepository.findByNameIgnoreCase(request.city().trim()).orElse(null);
        if (city == null) {
            return emptyPage(request);
        }
        UUID localityId = null;
        if (request.locality() != null && !request.locality().isBlank()) {
            Locality locality = localityRepository.findByCityAndNameIgnoreCase(city, request.locality().trim())
                    .orElse(null);
            if (locality == null) {
                return emptyPage(request);
            }
            localityId = locality.getId();
        }

        List<Property> properties = propertyRepository.findForSearch(city.getId(), localityId, request.kind(),
                request.minStars());
        if (properties.isEmpty()) {
            return emptyPage(request);
        }
        Map<Property, List<RoomType>> roomTypes = roomTypeRepository.findWithVariantsByPropertyIn(properties).stream()
                .collect(Collectors.groupingBy(RoomType::getProperty));
        List<Variant> variants = roomTypes.values().stream()
                .flatMap(Collection::stream)
                .flatMap(roomType -> roomType.getVariants().stream())
                .filter(Variant::isActive)
                .toList();
        Map<UUID, Integer> available = inventoryService.availableRooms(variants, request.checkIn(), request.checkOut());

        List<SearchCandidate> matches = new ArrayList<>();
        for (Property property : properties) {
            List<Offer> offers = roomTypes.getOrDefault(property, List.of()).stream()
                    .flatMap(roomType -> roomType.getVariants().stream())
                    .filter(Variant::isActive)
                    .map(variant -> offer(variant, available.getOrDefault(variant.getId(), 0), request))
                    .toList();
            SearchCandidate candidate = new SearchCandidate(property, offers);
            for (SearchFilter filter : filters) {
                candidate = filter.apply(candidate, request);
            }
            if (!candidate.offers().isEmpty()) {
                matches.add(candidate);
            }
        }

        matches.sort(comparator(request));
        List<PropertySearchResultResponse> page = matches.stream()
                .skip((long) request.page() * request.size())
                .limit(request.size())
                .map(this::toResponse)
                .toList();
        return PageResponse.of(page, request.page(), request.size(), matches.size());
    }

    private Offer offer(Variant variant, int availableRooms, PropertySearchRequest request) {
        List<NightlyRate> rates = pricingStrategy.nightlyRates(variant, request.checkIn(), request.checkOut());
        Money perRoom = rates.stream().map(NightlyRate::perRoom).reduce(Money::plus).orElseThrow();
        BigDecimal average = perRoom.amount().divide(BigDecimal.valueOf(rates.size()), 2, RoundingMode.HALF_UP);
        return new Offer(variant, availableRooms, perRoom.times(request.rooms()),
                Money.of(average, perRoom.currency()));
    }

    private static Comparator<SearchCandidate> comparator(PropertySearchRequest request) {
        Comparator<SearchCandidate> byPrice = Comparator.comparing(candidate -> candidate.lowestTotal().amount());
        return switch (request.sort()) {
            case PRICE_ASC -> byPrice;
            case PRICE_DESC -> byPrice.reversed();
            case RATING_DESC -> Comparator.comparing((SearchCandidate candidate) -> candidate.property().getStarRating(),
                    Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(byPrice);
        };
    }

    private PropertySearchResultResponse toResponse(SearchCandidate candidate) {
        Property property = candidate.property();
        List<OfferResponse> offers = candidate.offers().stream()
                .sorted(Comparator.comparing(offer -> offer.total().amount()))
                .map(offer -> new OfferResponse(
                        offer.variant().getId(),
                        offer.variant().getRoomType().getName(),
                        offer.variant().getName(),
                        offer.variant().getRoomType().getMaxOccupancy(),
                        offer.availableRooms(),
                        MoneyResponse.from(offer.averageNight()),
                        MoneyResponse.from(offer.total()),
                        sortedCodes(offer.variant().allFeatures())))
                .toList();
        return new PropertySearchResultResponse(
                property.getId(),
                property.getName(),
                property.getKind(),
                property.getStarRating(),
                property.getAddress().locality().getName(),
                property.getAddress().locality().getCity().getName(),
                sortedCodes(property.getAmenities()),
                MoneyResponse.from(candidate.lowestTotal()),
                offers);
    }

    private static List<String> sortedCodes(Collection<Amenity> amenities) {
        return amenities.stream().map(Amenity::getCode).sorted().toList();
    }

    private static PageResponse<PropertySearchResultResponse> emptyPage(PropertySearchRequest request) {
        return PageResponse.of(List.of(), request.page(), request.size(), 0);
    }
}
