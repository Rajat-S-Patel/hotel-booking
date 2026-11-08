package com.rajat.hotelbooking.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddressRequest(
        @NotBlank @Size(max = 255) String line,
        @NotBlank @Pattern(regexp = "[1-9][0-9]{5}", message = "must be 6 digits and not start with 0") String pincode,
        @NotNull UUID localityId) {
}
