package com.rajat.hotelbooking.property.dto;

import com.rajat.hotelbooking.property.entity.enums.OwnerKind;

import java.util.UUID;

public record OwnerResponse(
        UUID id,
        String name,
        OwnerKind kind,
        String contactEmail,
        String contactPhone) {
}
