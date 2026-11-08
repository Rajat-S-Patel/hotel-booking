package com.rajat.hotelbooking.reference.service;

import com.rajat.hotelbooking.common.exception.NotFoundException;
import com.rajat.hotelbooking.reference.dto.AmenityResponse;
import com.rajat.hotelbooking.reference.dto.CityResponse;
import com.rajat.hotelbooking.reference.dto.LocalityResponse;
import com.rajat.hotelbooking.reference.entity.Amenity;
import com.rajat.hotelbooking.reference.entity.City;
import com.rajat.hotelbooking.reference.entity.enums.AmenityScope;
import com.rajat.hotelbooking.reference.mapper.ReferenceMapper;
import com.rajat.hotelbooking.reference.repository.AmenityRepository;
import com.rajat.hotelbooking.reference.repository.CityRepository;
import com.rajat.hotelbooking.reference.repository.LocalityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceDataService {

    private final CityRepository cityRepository;
    private final LocalityRepository localityRepository;
    private final AmenityRepository amenityRepository;
    private final ReferenceMapper referenceMapper;

    public List<CityResponse> listCities() {
        return cityRepository.findAllByOrderByNameAsc().stream()
                .map(referenceMapper::toResponse)
                .toList();
    }

    public List<LocalityResponse> listLocalities(UUID cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> NotFoundException.of("city", cityId));
        return localityRepository.findByCityOrderByNameAsc(city).stream()
                .map(referenceMapper::toResponse)
                .toList();
    }

    public List<AmenityResponse> listAmenities(AmenityScope scope) {
        List<Amenity> amenities = new ArrayList<>();
        for (AmenityScope each : AmenityScope.values()) {
            if (scope == null || scope == each) {
                amenities.addAll(amenityRepository.findByScopeOrderByCodeAsc(each));
            }
        }
        return referenceMapper.toAmenityResponses(amenities);
    }
}
