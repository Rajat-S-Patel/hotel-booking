package com.rajat.hotelbooking.property.rules;

import com.rajat.hotelbooking.property.dto.CreatePropertyRequest;
import com.rajat.hotelbooking.property.dto.CreateRoomTypeRequest;
import com.rajat.hotelbooking.property.dto.CreateVariantRequest;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;

public interface PropertyKindRules {

    PropertyKind kind();

    void validate(CreatePropertyRequest request);

    static int totalRooms(CreatePropertyRequest request) {
        return request.roomTypes().stream().mapToInt(PropertyKindRules::totalRooms).sum();
    }

    static int totalRooms(CreateRoomTypeRequest roomType) {
        int extra = roomType.variants() == null ? 0
                : roomType.variants().stream().mapToInt(CreateVariantRequest::roomCount).sum();
        return roomType.standardRoomCount() + extra;
    }
}
