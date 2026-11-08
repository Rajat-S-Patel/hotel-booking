package com.rajat.hotelbooking.property.mapper;

import com.rajat.hotelbooking.common.dto.MoneyResponse;
import com.rajat.hotelbooking.common.mapper.MoneyMapper;
import com.rajat.hotelbooking.policy.mapper.PolicyMapper;
import com.rajat.hotelbooking.property.dto.AddressResponse;
import com.rajat.hotelbooking.property.dto.OwnerResponse;
import com.rajat.hotelbooking.property.dto.PropertyResponse;
import com.rajat.hotelbooking.property.dto.PropertySummaryResponse;
import com.rajat.hotelbooking.property.dto.RoomTypeResponse;
import com.rajat.hotelbooking.property.dto.VariantResponse;
import com.rajat.hotelbooking.property.entity.Address;
import com.rajat.hotelbooking.property.entity.Owner;
import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.RoomType;
import com.rajat.hotelbooking.property.entity.Variant;
import com.rajat.hotelbooking.reference.dto.AmenityResponse;
import com.rajat.hotelbooking.reference.mapper.ReferenceMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.ZoneId;
import java.util.List;

@Mapper(uses = {MoneyMapper.class, ReferenceMapper.class, PolicyMapper.class})
public interface PropertyMapper {

    @Mapping(target = "properties", source = "properties")
    OwnerResponse toResponse(Owner owner, List<Property> properties);

    @Mapping(target = "locality", source = "address.locality.name")
    @Mapping(target = "city", source = "address.locality.city.name")
    PropertySummaryResponse toSummary(Property property);

    @Mapping(target = "id", source = "property.id")
    @Mapping(target = "ownerId", source = "property.owner.id")
    @Mapping(target = "name", source = "property.name")
    @Mapping(target = "kind", source = "property.kind")
    @Mapping(target = "starRating", source = "property.starRating")
    @Mapping(target = "address", source = "property.address")
    @Mapping(target = "checkInTime", source = "property.checkInTime")
    @Mapping(target = "checkOutTime", source = "property.checkOutTime")
    @Mapping(target = "timeZone", source = "property.timeZone")
    @Mapping(target = "acceptedBookingTypes", source = "property.acceptedBookingTypes")
    @Mapping(target = "amenities", source = "property.amenities")
    @Mapping(target = "cancellationPolicy", source = "property.cancellationPolicy")
    @Mapping(target = "active", source = "property.active")
    @Mapping(target = "roomTypes", source = "roomTypes")
    PropertyResponse toResponse(Property property, List<RoomType> roomTypes);

    @Mapping(target = "localityId", source = "locality.id")
    @Mapping(target = "locality", source = "locality.name")
    @Mapping(target = "cityId", source = "locality.city.id")
    @Mapping(target = "city", source = "locality.city.name")
    AddressResponse toResponse(Address address);

    @Mapping(target = "totalRooms", source = ".", qualifiedByName = "totalRooms")
    RoomTypeResponse toResponse(RoomType roomType);

    @Mapping(target = "pricePerNight", source = ".", qualifiedByName = "pricePerNight")
    @Mapping(target = "features", source = ".", qualifiedByName = "allFeatures")
    VariantResponse toResponse(Variant variant);

    @Named("totalRooms")
    default int totalRooms(RoomType roomType) {
        return roomType.totalRooms();
    }

    @Named("pricePerNight")
    default MoneyResponse pricePerNight(Variant variant) {
        return MoneyResponse.from(variant.pricePerNight());
    }

    @Named("allFeatures")
    default List<AmenityResponse> allFeatures(Variant variant) {
        return AmenityResponse.sortedFrom(variant.allFeatures());
    }

    default String zoneId(ZoneId zoneId) {
        return zoneId == null ? null : zoneId.getId();
    }
}
