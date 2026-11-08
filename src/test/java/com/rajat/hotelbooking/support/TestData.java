package com.rajat.hotelbooking.support;

import com.rajat.hotelbooking.booking.entity.Booking;
import com.rajat.hotelbooking.booking.entity.BookingDraft;
import com.rajat.hotelbooking.booking.entity.Customer;
import com.rajat.hotelbooking.common.entity.BaseEntity;
import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.common.entity.enums.BookingType;
import com.rajat.hotelbooking.policy.entity.Policy;
import com.rajat.hotelbooking.policy.entity.PolicyTier;
import com.rajat.hotelbooking.policy.entity.enums.PolicyKind;
import com.rajat.hotelbooking.pricing.BaseRatePricing;
import com.rajat.hotelbooking.property.entity.Address;
import com.rajat.hotelbooking.property.entity.Owner;
import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.RoomType;
import com.rajat.hotelbooking.property.entity.Variant;
import com.rajat.hotelbooking.property.entity.enums.OwnerKind;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import com.rajat.hotelbooking.reference.entity.Amenity;
import com.rajat.hotelbooking.reference.entity.City;
import com.rajat.hotelbooking.reference.entity.Locality;
import com.rajat.hotelbooking.reference.entity.enums.AmenityScope;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Real entities built in memory, with a fixed clock (12:00 IST on 2026-11-08).
public final class TestData {

    public static final Instant NOW = Instant.parse("2026-11-08T06:30:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    public static final Duration HOLD = Duration.ofMinutes(15);

    private TestData() {
    }

    public static <T extends BaseEntity> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    public static Money inr(String amount) {
        return Money.of(new BigDecimal(amount), Money.INR);
    }

    public static Amenity amenity(String code, AmenityScope scope) {
        return withId(new Amenity(code, code.toLowerCase(), scope));
    }

    public static Locality locality() {
        return withId(new Locality(withId(new City("Ahmedabad")), "Satellite"));
    }

    public static Owner owner() {
        return withId(new Owner("Lakeview Hotels", OwnerKind.GROUP, "owner@lakeview.example", "9876543210"));
    }

    public static Policy moderatePolicy() {
        return withId(Policy.platformPolicy("Moderate", PolicyKind.TIERED,
                List.of(new PolicyTier(72, 100), new PolicyTier(24, 50))));
    }

    public static Property property() {
        return property(owner(), moderatePolicy());
    }

    public static Property property(Owner owner, Policy policy) {
        return withId(new Property(owner, "Lakeview Grand", PropertyKind.HOTEL, 4,
                new Address("Opp. Vastrapur Lake", "380015", locality()),
                LocalTime.NOON, LocalTime.of(11, 0), ZoneId.of("Asia/Kolkata"),
                Set.of(BookingType.PREPAID, BookingType.PAY_AT_PROPERTY), policy));
    }

    public static RoomType roomType(Property property, int rooms) {
        RoomType roomType = withId(new RoomType(property, "Deluxe", 2, inr("3000"), rooms));
        roomType.getVariants().forEach(TestData::withId);
        return roomType;
    }

    // the Standard variant of a fresh Deluxe room type (max 2 guests, 3000 a night)
    public static Variant variant(int rooms) {
        return roomType(property(), rooms).getVariants().getFirst();
    }

    public static Customer customer() {
        return withId(new Customer("Ravi Shah", "ravi@example.com", "9811111111"));
    }

    public static BookingDraft draft(Variant variant, BookingType type, LocalDate checkIn, LocalDate checkOut,
                                     int rooms, int guests) {
        Property property = variant.getRoomType().getProperty();
        return new BookingDraft(customer(), property, variant,
                property.getCancellationPolicy().getCurrentVersion(), type, checkIn, checkOut, rooms, guests, null,
                new BaseRatePricing().nightlyRates(variant, checkIn, checkOut), "key-" + UUID.randomUUID(), "fp");
    }

    public static Booking pendingBooking(Variant variant, LocalDate checkIn, LocalDate checkOut) {
        return withId(Booking.awaitingPayment(draft(variant, BookingType.PREPAID, checkIn, checkOut, 1, 2),
                NOW.plus(HOLD)));
    }
}
