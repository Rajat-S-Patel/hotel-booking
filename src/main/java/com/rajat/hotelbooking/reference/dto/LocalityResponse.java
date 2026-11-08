package com.rajat.hotelbooking.reference.dto;

import java.util.UUID;

public record LocalityResponse(UUID id, String name, UUID cityId, String cityName) {
}
