package com.rajat.hotelbooking.property.rules;

import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.property.dto.CreatePropertyRequest;
import com.rajat.hotelbooking.property.dto.CreateRoomTypeRequest;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import org.springframework.stereotype.Component;

@Component
public class VillaRules implements PropertyKindRules {

    @Override
    public PropertyKind kind() {
        return PropertyKind.VILLA;
    }

    @Override
    public void validate(CreatePropertyRequest request) {
        PreConditions.require(request.roomTypes().size() == 1, "a villa must have exactly one room type");
        CreateRoomTypeRequest unit = request.roomTypes().getFirst();
        PreConditions.require(unit.variants() == null || unit.variants().isEmpty(),
                "a villa cannot have extra variants");
        PreConditions.require(unit.standardRoomCount() == 1, "a villa is booked as a single unit");
    }
}
