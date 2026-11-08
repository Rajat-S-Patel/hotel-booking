package com.rajat.hotelbooking.search.service;

import com.rajat.hotelbooking.common.dto.PageResponse;
import com.rajat.hotelbooking.inventory.service.InventoryService;
import com.rajat.hotelbooking.pricing.BaseRatePricing;
import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.RoomType;
import com.rajat.hotelbooking.property.repository.PropertyRepository;
import com.rajat.hotelbooking.property.repository.RoomTypeRepository;
import com.rajat.hotelbooking.reference.entity.City;
import com.rajat.hotelbooking.reference.repository.CityRepository;
import com.rajat.hotelbooking.reference.repository.LocalityRepository;
import com.rajat.hotelbooking.search.dto.PropertySearchRequest;
import com.rajat.hotelbooking.search.dto.PropertySearchResultResponse;
import com.rajat.hotelbooking.search.dto.SearchSort;
import com.rajat.hotelbooking.search.filter.AmenityFilter;
import com.rajat.hotelbooking.search.filter.AvailabilityFilter;
import com.rajat.hotelbooking.search.filter.PriceRangeFilter;
import com.rajat.hotelbooking.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.rajat.hotelbooking.support.TestData.inr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertySearchServiceTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 3);

    @Mock
    private CityRepository cityRepository;
    @Mock
    private LocalityRepository localityRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private InventoryService inventoryService;

    private PropertySearchService service;
    private City city;
    private final Map<UUID, Integer> available = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new PropertySearchService(cityRepository, localityRepository, propertyRepository, roomTypeRepository,
                inventoryService, new BaseRatePricing(),
                List.of(new AvailabilityFilter(), new AmenityFilter(), new PriceRangeFilter()));
        city = TestData.locality().getCity();
    }

    @Test
    void unknownCityGivesAnEmptyPage() {
        when(cityRepository.findByNameIgnoreCase("Delhi")).thenReturn(Optional.empty());

        PageResponse<PropertySearchResultResponse> page = service.search(request("Delhi", SearchSort.PRICE_ASC, 0, 20));

        assertThat(page.content()).isEmpty();
        verify(propertyRepository, never()).findForSearch(any(), any(), any(), any());
    }

    @Test
    void soldOutPropertyIsLeftOut() {
        RoomType open = roomType("Open Hotel", 3, "3000", 2);
        RoomType soldOut = roomType("Sold Out Hotel", 4, "2000", 0);
        givenProperties(open, soldOut);

        PageResponse<PropertySearchResultResponse> page = service.search(request("Ahmedabad", SearchSort.PRICE_ASC, 0, 20));

        assertThat(page.content()).extracting(PropertySearchResultResponse::name).containsExactly("Open Hotel");
        PropertySearchResultResponse result = page.content().getFirst();
        assertThat(result.lowestTotalPrice().amount()).isEqualByComparingTo("6000");
        assertThat(result.offers().getFirst().availableRooms()).isEqualTo(2);
        assertThat(result.offers().getFirst().averagePricePerNight().amount()).isEqualByComparingTo("3000");
    }

    @Test
    void sortsByPriceOrRating() {
        RoomType cheap = roomType("Budget Inn", 2, "1500", 5);
        RoomType premium = roomType("Grand Palace", 5, "9000", 5);
        RoomType middle = roomType("City Stay", 4, "4000", 5);
        givenProperties(cheap, premium, middle);

        assertThat(namesSortedBy(SearchSort.PRICE_ASC)).containsExactly("Budget Inn", "City Stay", "Grand Palace");
        assertThat(namesSortedBy(SearchSort.PRICE_DESC)).containsExactly("Grand Palace", "City Stay", "Budget Inn");
        assertThat(namesSortedBy(SearchSort.RATING_DESC)).containsExactly("Grand Palace", "City Stay", "Budget Inn");
    }

    @Test
    void pagesTheSortedResults() {
        givenProperties(roomType("A", 3, "1000", 5), roomType("B", 3, "2000", 5), roomType("C", 3, "3000", 5));

        PageResponse<PropertySearchResultResponse> page = service.search(request("Ahmedabad", SearchSort.PRICE_ASC, 1, 2));

        assertThat(page.content()).extracting(PropertySearchResultResponse::name).containsExactly("C");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    private List<String> namesSortedBy(SearchSort sort) {
        return service.search(request("Ahmedabad", sort, 0, 20)).content().stream()
                .map(PropertySearchResultResponse::name).toList();
    }

    private RoomType roomType(String propertyName, int stars, String price, int roomsLeft) {
        Property property = TestData.property();
        ReflectionTestUtils.setField(property, "name", propertyName);
        ReflectionTestUtils.setField(property, "starRating", stars);
        RoomType roomType = TestData.withId(new RoomType(property, "Deluxe", 2, inr(price), 5));
        roomType.getVariants().forEach(TestData::withId);
        available.put(roomType.getVariants().getFirst().getId(), roomsLeft);
        return roomType;
    }

    private void givenProperties(RoomType... roomTypes) {
        List<Property> properties = Arrays.stream(roomTypes).map(RoomType::getProperty).toList();
        when(cityRepository.findByNameIgnoreCase("Ahmedabad")).thenReturn(Optional.of(city));
        when(propertyRepository.findForSearch(city.getId(), null, null, null)).thenReturn(properties);
        when(roomTypeRepository.findWithVariantsByPropertyIn(properties)).thenReturn(List.of(roomTypes));
        when(inventoryService.availableRooms(anyCollection(), eq(CHECK_IN), eq(CHECK_OUT))).thenReturn(available);
    }

    private static PropertySearchRequest request(String city, SearchSort sort, int page, int size) {
        return new PropertySearchRequest(city, null, CHECK_IN, CHECK_OUT, 2, 1, null, null, null, null, null, sort,
                page, size);
    }
}
