package com.rajat.hotelbooking.property.service;

import com.rajat.hotelbooking.common.exception.NotFoundException;
import com.rajat.hotelbooking.property.dto.PropertyResponse;
import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.mapper.PropertyMapper;
import com.rajat.hotelbooking.property.repository.PropertyRepository;
import com.rajat.hotelbooking.property.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyQueryService {

    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PropertyMapper propertyMapper;

    public PropertyResponse getProperty(UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> NotFoundException.of("property", propertyId));
        return propertyMapper.toResponse(property, roomTypeRepository.findByPropertyOrderByNameAsc(property));
    }
}
