package com.rajat.hotelbooking.bootstrap;

import com.rajat.hotelbooking.common.entity.enums.BookingType;
import com.rajat.hotelbooking.policy.entity.Policy;
import com.rajat.hotelbooking.policy.entity.PolicyTier;
import com.rajat.hotelbooking.policy.entity.enums.PolicyKind;
import com.rajat.hotelbooking.policy.repository.PolicyRepository;
import com.rajat.hotelbooking.property.dto.AddressRequest;
import com.rajat.hotelbooking.property.dto.CreatePropertyRequest;
import com.rajat.hotelbooking.property.dto.CreateRoomTypeRequest;
import com.rajat.hotelbooking.property.dto.CreateVariantRequest;
import com.rajat.hotelbooking.property.dto.OnboardOwnerRequest;
import com.rajat.hotelbooking.property.entity.enums.OwnerKind;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import com.rajat.hotelbooking.property.service.PropertyOnboardingService;
import com.rajat.hotelbooking.reference.entity.Amenity;
import com.rajat.hotelbooking.reference.entity.City;
import com.rajat.hotelbooking.reference.entity.Locality;
import com.rajat.hotelbooking.reference.entity.enums.AmenityScope;
import com.rajat.hotelbooking.reference.repository.AmenityRepository;
import com.rajat.hotelbooking.reference.repository.CityRepository;
import com.rajat.hotelbooking.reference.repository.LocalityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hotelbooking.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private final CityRepository cityRepository;
    private final LocalityRepository localityRepository;
    private final AmenityRepository amenityRepository;
    private final PolicyRepository policyRepository;
    private final PropertyOnboardingService onboardingService;
    private final TransactionTemplate transaction;

    private final Map<String, UUID> localities = new HashMap<>();
    private final Map<String, UUID> policies = new HashMap<>();

    @Override
    public void run(ApplicationArguments args) {
        if (!cityRepository.findAllByOrderByNameAsc().isEmpty()) {
            log.info("seed data already present, skipping");
            return;
        }
        transaction.executeWithoutResult(status -> {
            seedLocations();
            seedAmenities();
            seedPolicies();
        });
        seedProperties();
        log.info("seed data loaded");
    }

    private void seedLocations() {
        Map<String, List<String>> cities = Map.of(
                "Ahmedabad", List.of("SG Highway", "Navrangpura", "Satellite"),
                "Mumbai", List.of("Bandra", "Colaba", "Juhu"),
                "Goa", List.of("Calangute", "Panjim"),
                "Bengaluru", List.of("Indiranagar", "Koramangala"));
        cities.forEach((cityName, localityNames) -> {
            City city = cityRepository.save(new City(cityName));
            for (String name : localityNames) {
                localities.put(name, localityRepository.save(new Locality(city, name)).getId());
            }
        });
    }

    private void seedAmenities() {
        amenityRepository.save(new Amenity("WIFI", "Free Wi-Fi", AmenityScope.PROPERTY));
        amenityRepository.save(new Amenity("POOL", "Swimming pool", AmenityScope.PROPERTY));
        amenityRepository.save(new Amenity("PARKING", "Parking", AmenityScope.PROPERTY));
        amenityRepository.save(new Amenity("SPA", "Spa", AmenityScope.PROPERTY));
        amenityRepository.save(new Amenity("GYM", "Gym", AmenityScope.PROPERTY));
        amenityRepository.save(new Amenity("RESTAURANT", "Restaurant", AmenityScope.PROPERTY));
        amenityRepository.save(new Amenity("AIR_CONDITIONING", "Air conditioning", AmenityScope.ROOM));
        amenityRepository.save(new Amenity("SEA_VIEW", "Sea view", AmenityScope.ROOM));
        amenityRepository.save(new Amenity("BALCONY", "Balcony", AmenityScope.ROOM));
        amenityRepository.save(new Amenity("BATHTUB", "Bathtub", AmenityScope.ROOM));
    }

    private void seedPolicies() {
        savePolicy("Flexible", List.of(new PolicyTier(24, 100)));
        savePolicy("Moderate", List.of(new PolicyTier(72, 100), new PolicyTier(24, 50)));
        savePolicy("Strict", List.of(new PolicyTier(168, 50)));
    }

    private void savePolicy(String name, List<PolicyTier> tiers) {
        Policy policy = policyRepository.save(Policy.platformPolicy(name, PolicyKind.TIERED, tiers));
        policies.put(name, policy.getId());
    }

    private void seedProperties() {
        UUID coastline = onboardingService.onboardOwner(new OnboardOwnerRequest(
                "Coastline Hospitality", OwnerKind.GROUP, "ops@coastline.example", "+919820000001")).id();
        onboardingService.addProperty(coastline, seaBreezeHotel());
        onboardingService.addProperty(coastline, palmGroveResort());
        onboardingService.addProperty(coastline, skylineHotel());

        UUID asha = onboardingService.onboardOwner(new OnboardOwnerRequest(
                "Asha Mehta", OwnerKind.INDIVIDUAL, "asha.mehta@example.com", "+919820000002")).id();
        onboardingService.addProperty(asha, ashaHomestay());
        onboardingService.addProperty(asha, heritageVilla());
    }

    private CreatePropertyRequest seaBreezeHotel() {
        CreateRoomTypeRequest deluxe = new CreateRoomTypeRequest("Deluxe", 2, price("4500"), 5,
                Set.of("AIR_CONDITIONING"),
                List.of(new CreateVariantRequest("Sea View", price("1000"), 2, Set.of("SEA_VIEW", "BALCONY"))));
        CreateRoomTypeRequest suite = new CreateRoomTypeRequest("Suite", 4, price("9000"), 2,
                Set.of("AIR_CONDITIONING", "BATHTUB"), null);
        return property("Sea Breeze Hotel", PropertyKind.HOTEL, 4, "Juhu Tara Road", "400049", "Juhu",
                Set.of(BookingType.PREPAID, BookingType.PAY_AT_PROPERTY), "Moderate",
                Set.of("WIFI", "RESTAURANT", "GYM"), List.of(deluxe, suite));
    }

    private CreatePropertyRequest palmGroveResort() {
        CreateRoomTypeRequest cottage = new CreateRoomTypeRequest("Garden Cottage", 3, price("7000"), 6,
                Set.of("AIR_CONDITIONING"),
                List.of(new CreateVariantRequest("Pool Facing", price("1500"), 2, Set.of("BALCONY"))));
        return property("Palm Grove Resort", PropertyKind.RESORT, 5, "Calangute Beach Road", "403516", "Calangute",
                Set.of(BookingType.PREPAID), "Flexible",
                Set.of("WIFI", "POOL", "SPA", "RESTAURANT"), List.of(cottage));
    }

    private CreatePropertyRequest skylineHotel() {
        CreateRoomTypeRequest standard = new CreateRoomTypeRequest("Standard", 2, price("2800"), 10,
                Set.of("AIR_CONDITIONING"), null);
        return property("Hotel Skyline", PropertyKind.HOTEL, 3, "Near Iscon Cross Road", "380054", "SG Highway",
                Set.of(BookingType.PREPAID, BookingType.PAY_AT_PROPERTY), "Strict",
                Set.of("WIFI", "PARKING"), List.of(standard));
    }

    private CreatePropertyRequest ashaHomestay() {
        CreateRoomTypeRequest room = new CreateRoomTypeRequest("Double Room", 2, price("1800"), 3, null, null);
        return property("Asha's Homestay", PropertyKind.HOMESTAY, null, "12 Law Garden Lane", "380009",
                "Navrangpura", Set.of(BookingType.PREPAID, BookingType.PAY_AT_PROPERTY), "Flexible",
                Set.of("WIFI"), List.of(room));
    }

    private CreatePropertyRequest heritageVilla() {
        CreateRoomTypeRequest villa = new CreateRoomTypeRequest("Entire Villa", 8, price("15000"), 1,
                Set.of("AIR_CONDITIONING", "BALCONY"), null);
        return property("Fontainhas Heritage Villa", PropertyKind.VILLA, null, "31 Rua de Natal", "403001", "Panjim",
                Set.of(BookingType.PREPAID), "Strict", Set.of("WIFI", "PARKING", "POOL"), List.of(villa));
    }

    private CreatePropertyRequest property(String name, PropertyKind kind, Integer stars, String line, String pincode,
                                           String locality, Set<BookingType> bookingTypes, String policy,
                                           Set<String> amenities, List<CreateRoomTypeRequest> roomTypes) {
        return new CreatePropertyRequest(name, kind, stars, new AddressRequest(line, pincode, localities.get(locality)),
                null, null, null, bookingTypes, policies.get(policy), amenities, roomTypes);
    }

    private static BigDecimal price(String amount) {
        return new BigDecimal(amount);
    }
}
