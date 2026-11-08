package com.rajat.hotelbooking.property.rules;

import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.property.dto.CreatePropertyRequest;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import org.springframework.stereotype.Component;

@Component
public class ResortRules implements PropertyKindRules {

    @Override
    public PropertyKind kind() {
        return PropertyKind.RESORT;
    }

    @Override
    public void validate(CreatePropertyRequest request) {
        PreConditions.require(request.starRating() != null, "a resort must have a star rating");
        PreConditions.require(request.amenityCodes() != null && !request.amenityCodes().isEmpty(),
                "a resort must list at least one property amenity");
    }
}
