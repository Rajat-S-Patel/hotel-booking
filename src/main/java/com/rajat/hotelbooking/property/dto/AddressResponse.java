package com.rajat.hotelbooking.property.dto;

import java.util.UUID;

public record AddressResponse(String line, String pincode, UUID localityId, String locality, UUID cityId, String city) {
}
