package com.rajat.hotelbooking.property.dto;

import com.rajat.hotelbooking.common.entity.enums.BookingType;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record CreatePropertyRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull PropertyKind kind,
        @Min(1) @Max(5) Integer starRating,
        @NotNull @Valid AddressRequest address,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        ZoneId timeZone,
        Set<@NotNull BookingType> acceptedBookingTypes,
        @NotNull UUID cancellationPolicyId,
        @Size(max = 30) Set<@NotBlank String> amenityCodes,
        @NotEmpty @Size(max = 20) List<@Valid @NotNull CreateRoomTypeRequest> roomTypes) {

    @AssertTrue(message = "acceptedBookingTypes must include PREPAID")
    public boolean isPrepaidAccepted() {
        return acceptedBookingTypes == null || acceptedBookingTypes.isEmpty()
                || acceptedBookingTypes.contains(BookingType.PREPAID);
    }

    @AssertTrue(message = "room type names must be unique within a property")
    public boolean isRoomTypeNamesUnique() {
        if (roomTypes == null) {
            return true;
        }
        List<String> names = roomTypes.stream()
                .filter(Objects::nonNull)
                .map(CreateRoomTypeRequest::name)
                .filter(Objects::nonNull)
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .toList();
        return names.stream().distinct().count() == names.size();
    }
}
