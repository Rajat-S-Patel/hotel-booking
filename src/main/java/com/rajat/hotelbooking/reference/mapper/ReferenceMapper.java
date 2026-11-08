package com.rajat.hotelbooking.reference.mapper;

import com.rajat.hotelbooking.reference.dto.AmenityResponse;
import com.rajat.hotelbooking.reference.dto.CityResponse;
import com.rajat.hotelbooking.reference.dto.LocalityResponse;
import com.rajat.hotelbooking.reference.entity.Amenity;
import com.rajat.hotelbooking.reference.entity.City;
import com.rajat.hotelbooking.reference.entity.Locality;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ReferenceMapper {

    CityResponse toResponse(City city);

    @Mapping(target = "cityId", source = "city.id")
    @Mapping(target = "cityName", source = "city.name")
    LocalityResponse toResponse(Locality locality);

    default AmenityResponse toResponse(Amenity amenity) {
        return AmenityResponse.from(amenity);
    }

    default List<AmenityResponse> toAmenityResponses(Collection<Amenity> amenities) {
        return AmenityResponse.sortedFrom(amenities);
    }
}
