package com.rajat.hotelbooking.booking.dto;

import com.rajat.hotelbooking.common.dto.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuestRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE) String phone) {
}
