package com.rajat.hotelbooking.property.dto;

import com.rajat.hotelbooking.property.entity.Variant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record CreateRoomTypeRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(1) @Max(20) Integer maxOccupancy,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal basePrice,
        @NotNull @Min(1) @Max(1000) Integer standardRoomCount,
        @Size(max = 30) Set<@NotBlank String> featureCodes,
        @Size(max = 10) List<@Valid @NotNull CreateVariantRequest> variants) {

    @AssertTrue(message = "variant names must be unique within a room type and must not be 'Standard'")
    public boolean isVariantNamesValid() {
        if (variants == null) {
            return true;
        }
        List<String> names = variants.stream()
                .filter(Objects::nonNull)
                .map(CreateVariantRequest::name)
                .filter(Objects::nonNull)
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .toList();
        return names.stream().distinct().count() == names.size()
                && !names.contains(Variant.STANDARD_NAME.toLowerCase(Locale.ROOT));
    }
}
