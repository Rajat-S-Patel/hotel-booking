package com.rajat.hotelbooking.property.rules;

import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.property.dto.CreatePropertyRequest;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import org.springframework.stereotype.Component;

@Component
public class HotelRules implements PropertyKindRules {

    @Override
    public PropertyKind kind() {
        return PropertyKind.HOTEL;
    }

    @Override
    public void validate(CreatePropertyRequest request) {
        PreConditions.require(request.starRating() != null, "a hotel must have a star rating");
    }
}
