package com.rajat.hotelbooking.property.dto;

import com.rajat.hotelbooking.common.dto.ValidationPatterns;
import com.rajat.hotelbooking.property.entity.enums.OwnerKind;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OnboardOwnerRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull OwnerKind kind,
        @NotBlank @Email @Size(max = 200) String contactEmail,
        @NotBlank @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE) String contactPhone) {
}
