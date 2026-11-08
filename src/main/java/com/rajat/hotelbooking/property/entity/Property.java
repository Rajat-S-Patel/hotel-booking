package com.rajat.hotelbooking.property.entity;


import com.rajat.hotelbooking.common.entity.VersionedEntity;
import com.rajat.hotelbooking.common.entity.enums.BookingType;
import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import com.rajat.hotelbooking.reference.entity.Amenity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Table(name = "property")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Property extends VersionedEntity {
    public static final LocalTime DEFAULT_CHECK_IN_TIME = LocalTime.of(12, 0);
    public static final LocalTime DEFAULT_CHECK_OUT_TIME = LocalTime.of(11, 0);
    public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("Asia/Kolkata");

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PropertyKind kind;

    @Column
    private Integer starRating;

    @Embedded
    private Address address;

    @Column(nullable = false)
    private LocalTime checkInTime;

    @Column(nullable = false)
    private LocalTime checkOutTime;

    @Column(nullable = false, length = 50)
    private ZoneId timeZone;

    @Getter(AccessLevel.NONE)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "property_booking_type",
            joinColumns = @JoinColumn(name = "property_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_property_booking_type",
                    columnNames = {"property_id", "booking_type"}))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "booking_type", nullable = false, length = 30)
    private Set<BookingType> acceptedBookingTypes = new HashSet<>();

    @Getter(AccessLevel.NONE)
    @ManyToMany
    @JoinTable(name = "property_amenity",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private Set<Amenity> amenities = new HashSet<>();

    @Column(nullable = false)
    private boolean active;

    public Property(Owner owner, String name, PropertyKind kind, Integer starRating, Address address,
                    LocalTime checkInTime, LocalTime checkOutTime, ZoneId timeZone,
                    Set<BookingType> acceptedBookingTypes) {
        this.owner = PreConditions.requireNonNull(owner, "owner");
        this.name = PreConditions.requireText(name, "property name");
        this.kind = PreConditions.requireNonNull(kind, "property kind");
        this.starRating = validateStarRating(starRating);
        this.address = PreConditions.requireNonNull(address, "address");
        this.checkInTime = checkInTime != null ? checkInTime : DEFAULT_CHECK_IN_TIME;
        this.checkOutTime = checkOutTime != null ? checkOutTime : DEFAULT_CHECK_OUT_TIME;
        this.timeZone = timeZone != null ? timeZone : DEFAULT_TIME_ZONE;
        changeAcceptedBookingTypes(acceptedBookingTypes);
        this.active = true;
    }

    private static Integer validateStarRating(Integer starRating) {
        if (starRating != null) {
            PreConditions.require(starRating >= 1 && starRating <= 5,
                    "star rating must be between 1 and 5: " + starRating);
        }
        return starRating;
    }

    public Set<BookingType> getAcceptedBookingTypes() {
        return Collections.unmodifiableSet(acceptedBookingTypes);
    }

    public Set<Amenity> getAmenities() {
        return Collections.unmodifiableSet(amenities);
    }

    public boolean accepts(BookingType bookingType) {
        return acceptedBookingTypes.contains(bookingType);
    }

    public void changeAcceptedBookingTypes(Set<BookingType> bookingTypes) {
        Set<BookingType> requested = bookingTypes == null || bookingTypes.isEmpty()
                ? Set.of(BookingType.PREPAID)
                : bookingTypes;
        PreConditions.require(requested.contains(BookingType.PREPAID),
                "a property must always accept PREPAID bookings");
        acceptedBookingTypes.clear();
        acceptedBookingTypes.addAll(requested);
    }

    public void addAmenity(Amenity amenity) {
        PreConditions.requireNonNull(amenity, "amenity");
        PreConditions.require(!amenity.isRoomFeature(),
                "room feature cannot be added as a property amenity: " + amenity.getCode());
        amenities.add(amenity);
    }

    public void removeAmenity(Amenity amenity) {
        amenities.remove(amenity);
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public Instant checkInMoment(LocalDate checkInDate) {
        return checkInDate.atTime(checkInTime).atZone(timeZone).toInstant();
    }
}
