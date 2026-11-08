package com.rajat.hotelbooking.property.service;

import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.common.exception.DomainValidationException;
import com.rajat.hotelbooking.common.exception.NotFoundException;
import com.rajat.hotelbooking.common.util.ContactDetails;
import com.rajat.hotelbooking.policy.entity.Policy;
import com.rajat.hotelbooking.policy.repository.PolicyRepository;
import com.rajat.hotelbooking.property.dto.CreatePropertyRequest;
import com.rajat.hotelbooking.property.dto.CreateRoomTypeRequest;
import com.rajat.hotelbooking.property.dto.CreateVariantRequest;
import com.rajat.hotelbooking.property.dto.OnboardOwnerRequest;
import com.rajat.hotelbooking.property.dto.OwnerResponse;
import com.rajat.hotelbooking.property.dto.PropertyResponse;
import com.rajat.hotelbooking.property.entity.Address;
import com.rajat.hotelbooking.property.entity.Owner;
import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.RoomType;
import com.rajat.hotelbooking.property.entity.Variant;
import com.rajat.hotelbooking.property.mapper.PropertyMapper;
import com.rajat.hotelbooking.property.repository.OwnerRepository;
import com.rajat.hotelbooking.property.repository.PropertyRepository;
import com.rajat.hotelbooking.property.repository.RoomTypeRepository;
import com.rajat.hotelbooking.property.rules.PropertyKindRulesRegistry;
import com.rajat.hotelbooking.reference.entity.Amenity;
import com.rajat.hotelbooking.reference.entity.Locality;
import com.rajat.hotelbooking.reference.repository.AmenityRepository;
import com.rajat.hotelbooking.reference.repository.LocalityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyOnboardingService {

    private final OwnerRepository ownerRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final LocalityRepository localityRepository;
    private final AmenityRepository amenityRepository;
    private final PolicyRepository policyRepository;
    private final PropertyKindRulesRegistry kindRules;
    private final PropertyMapper propertyMapper;

    @Transactional
    public OwnerResponse onboardOwner(OnboardOwnerRequest request) {
        String email = ContactDetails.normaliseEmail(request.contactEmail(), "contact email");
        if (ownerRepository.existsByContactEmail(email)) {
            throw new ConflictException("an owner with contact email " + email + " already exists");
        }
        Owner owner = ownerRepository.save(
                new Owner(request.name(), request.kind(), email, request.contactPhone()));
        return propertyMapper.toResponse(owner);
    }

    @Transactional
    public PropertyResponse addProperty(UUID ownerId, CreatePropertyRequest request) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> NotFoundException.of("owner", ownerId));
        kindRules.forKind(request.kind()).validate(request);
        Locality locality = localityRepository.findById(request.address().localityId())
                .orElseThrow(() -> NotFoundException.of("locality", request.address().localityId()));
        Policy policy = policyRepository.findById(request.cancellationPolicyId())
                .orElseThrow(() -> NotFoundException.of("cancellation policy", request.cancellationPolicyId()));
        Map<String, Amenity> amenities = resolveAmenities(request);

        Property property = new Property(owner, request.name(), request.kind(), request.starRating(),
                new Address(request.address().line(), request.address().pincode(), locality),
                request.checkInTime(), request.checkOutTime(), request.timeZone(),
                request.acceptedBookingTypes(), policy);
        codes(request.amenityCodes()).forEach(code -> property.addAmenity(amenities.get(code)));
        propertyRepository.save(property);

        List<RoomType> roomTypes = new ArrayList<>();
        for (CreateRoomTypeRequest roomTypeRequest : request.roomTypes()) {
            roomTypes.add(roomTypeRepository.save(createRoomType(property, roomTypeRequest, amenities)));
        }
        return propertyMapper.toResponse(property, roomTypes);
    }

    private RoomType createRoomType(Property property, CreateRoomTypeRequest request, Map<String, Amenity> amenities) {
        RoomType roomType = new RoomType(property, request.name(), request.maxOccupancy(),
                Money.of(request.basePrice(), Money.INR), request.standardRoomCount());
        codes(request.featureCodes()).forEach(code -> roomType.addBaseFeature(amenities.get(code)));
        if (request.variants() != null) {
            for (CreateVariantRequest variantRequest : request.variants()) {
                Variant variant = roomType.addVariant(variantRequest.name(),
                        Money.of(variantRequest.surcharge(), Money.INR), variantRequest.roomCount());
                codes(variantRequest.featureCodes()).forEach(code -> variant.addExtraFeature(amenities.get(code)));
            }
        }
        return roomType;
    }

    private Map<String, Amenity> resolveAmenities(CreatePropertyRequest request) {
        Set<String> requested = new TreeSet<>(codes(request.amenityCodes()));
        for (CreateRoomTypeRequest roomType : request.roomTypes()) {
            requested.addAll(codes(roomType.featureCodes()));
            if (roomType.variants() != null) {
                roomType.variants().forEach(variant -> requested.addAll(codes(variant.featureCodes())));
            }
        }
        if (requested.isEmpty()) {
            return Map.of();
        }
        Map<String, Amenity> found = amenityRepository.findByCodeIn(requested).stream()
                .collect(Collectors.toMap(Amenity::getCode, Function.identity()));
        Set<String> unknown = new TreeSet<>(requested);
        unknown.removeAll(found.keySet());
        if (!unknown.isEmpty()) {
            throw new DomainValidationException("unknown amenity codes: " + String.join(", ", unknown));
        }
        return found;
    }

    private static Set<String> codes(Set<String> codes) {
        return codes == null ? Set.of() : codes.stream().map(String::trim).collect(Collectors.toSet());
    }
}
