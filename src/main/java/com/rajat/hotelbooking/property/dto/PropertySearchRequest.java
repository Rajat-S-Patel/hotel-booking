package com.rajat.hotelbooking.property.dto;

import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public record PropertySearchRequest(
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String locality,
        @NotNull @FutureOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
        @NotNull @Min(1) @Max(50) Integer guests,
        @Min(1) @Max(10) Integer rooms,
        @DecimalMin("0.00") BigDecimal minPrice,
        @DecimalMin("0.00") BigDecimal maxPrice,
        @Size(max = 20) Set<@NotBlank String> amenities,
        @Min(1) @Max(5) Integer minStars,
        PropertyKind kind,
        SearchSort sort,
        @Min(0) Integer page,
        @Min(1) @Max(50) Integer size) {

    public static final int MAX_NIGHTS = 30;

    public PropertySearchRequest {
        rooms = rooms == null ? 1 : rooms;
        sort = sort == null ? SearchSort.PRICE_ASC : sort;
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }

    @AssertTrue(message = "checkOut must be after checkIn")
    public boolean isCheckOutAfterCheckIn() {
        return checkIn == null || checkOut == null || checkOut.isAfter(checkIn);
    }

    @AssertTrue(message = "stay must be at most " + MAX_NIGHTS + " nights")
    public boolean isStayWithinLimit() {
        return checkIn == null || checkOut == null || ChronoUnit.DAYS.between(checkIn, checkOut) <= MAX_NIGHTS;
    }

    @AssertTrue(message = "minPrice must not be greater than maxPrice")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }
}
