package com.rajat.hotelbooking.property.rules;

import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.property.dto.CreatePropertyRequest;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import org.springframework.stereotype.Component;

@Component
public class HomestayRules implements PropertyKindRules {

    static final int MAX_ROOMS = 10;

    @Override
    public PropertyKind kind() {
        return PropertyKind.HOMESTAY;
    }

    @Override
    public void validate(CreatePropertyRequest request) {
        PreConditions.require(request.starRating() == null, "a homestay does not have a star rating");
        int rooms = PropertyKindRules.totalRooms(request);
        PreConditions.require(rooms <= MAX_ROOMS,
                "a homestay can have at most " + MAX_ROOMS + " rooms, got " + rooms);
    }
}
