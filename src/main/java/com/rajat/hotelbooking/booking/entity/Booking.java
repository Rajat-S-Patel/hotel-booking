package com.rajat.hotelbooking.booking.entity;

import com.rajat.hotelbooking.booking.entity.enums.BookingStatus;
import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.common.entity.VersionedEntity;
import com.rajat.hotelbooking.common.entity.enums.BookingType;
import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.policy.entity.PolicyVersion;
import com.rajat.hotelbooking.pricing.NightlyRate;
import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.Variant;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(name = "booking",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_booking_reference", columnNames = "reference"),
                @UniqueConstraint(name = "uk_booking_idempotency_key", columnNames = "idempotency_key")},
        indexes = @Index(name = "ix_booking_status_hold_expires_at", columnList = "status, hold_expires_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking extends VersionedEntity {

    @Column(nullable = false, updatable = false, length = 10)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false, updatable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false, updatable = false)
    private Variant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_version_id", nullable = false, updatable = false)
    private PolicyVersion policyVersion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, updatable = false, length = 30)
    private BookingType bookingType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private BookingStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDate checkInDate;

    @Column(nullable = false, updatable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false, updatable = false)
    private int roomCount;

    @Column(nullable = false, updatable = false)
    private int guestCount;

    @Embedded
    private GuestDetails guestDetails;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_amount", nullable = false, updatable = false, precision = 12, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "total_currency", nullable = false, updatable = false, length = 3))
    private Money total;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    private Instant confirmedAt;

    private Instant cancelledAt;

    private Instant expiredAt;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false, length = 64)
    private String requestFingerprint;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "booking", cascade = CascadeType.PERSIST)
    @OrderBy("night ASC")
    private List<NightlyPriceLine> priceLines = new ArrayList<>();

    private Booking(BookingDraft draft) {
        PreConditions.requireNonNull(draft, "booking draft");
        this.reference = BookingReference.generate();
        this.customer = PreConditions.requireNonNull(draft.customer(), "customer");
        this.property = PreConditions.requireNonNull(draft.property(), "property");
        this.variant = PreConditions.requireNonNull(draft.variant(), "variant");
        this.policyVersion = PreConditions.requireNonNull(draft.policyVersion(), "policy version");
        this.bookingType = PreConditions.requireNonNull(draft.bookingType(), "booking type");
        this.checkInDate = PreConditions.requireNonNull(draft.checkInDate(), "check-in date");
        this.checkOutDate = PreConditions.requireNonNull(draft.checkOutDate(), "check-out date");
        this.roomCount = draft.roomCount();
        this.guestCount = draft.guestCount();
        this.guestDetails = draft.guestDetails();
        this.idempotencyKey = PreConditions.requireText(draft.idempotencyKey(), "idempotency key");
        this.requestFingerprint = PreConditions.requireText(draft.requestFingerprint(), "request fingerprint");
        validateStay();
        validateOffer();
        this.total = addPriceLines(PreConditions.requireNonNull(draft.nightlyRates(), "nightly rates"));
    }

    public static Booking awaitingPayment(BookingDraft draft, Instant holdExpiresAt) {
        Booking booking = new Booking(draft);
        booking.status = BookingStatus.PENDING_PAYMENT;
        booking.holdExpiresAt = PreConditions.requireNonNull(holdExpiresAt, "hold expiry");
        return booking;
    }

    public static Booking confirmedWithoutPayment(BookingDraft draft, Instant confirmedAt) {
        Booking booking = new Booking(draft);
        booking.status = BookingStatus.CONFIRMED;
        booking.confirmedAt = PreConditions.requireNonNull(confirmedAt, "confirmation time");
        return booking;
    }

    public void confirmPayment(Instant now) {
        requireTransitionTo(BookingStatus.CONFIRMED);
        this.status = BookingStatus.CONFIRMED;
        this.confirmedAt = PreConditions.requireNonNull(now, "now");
    }

    public BookingStatus cancel(Instant now) {
        PreConditions.requireNonNull(now, "now");
        requireTransitionTo(BookingStatus.CANCELLED);
        if (!now.isBefore(checkInMoment())) {
            throw new ConflictException("booking " + reference + " cannot be cancelled after check-in");
        }
        BookingStatus previous = status;
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = now;
        return previous;
    }

    public void expire(Instant now) {
        PreConditions.requireNonNull(now, "now");
        requireTransitionTo(BookingStatus.EXPIRED);
        if (!isHoldExpired(now)) {
            throw new ConflictException("booking " + reference + " hold has not expired yet");
        }
        this.status = BookingStatus.EXPIRED;
        this.expiredAt = now;
    }

    public boolean isHoldExpired(Instant now) {
        return status == BookingStatus.PENDING_PAYMENT && holdExpiresAt != null && !now.isBefore(holdExpiresAt);
    }

    public boolean isAwaitingPayment() {
        return status == BookingStatus.PENDING_PAYMENT;
    }

    public boolean holdsInventory() {
        return status.holdsInventory();
    }

    public Instant checkInMoment() {
        return property.checkInMoment(checkInDate);
    }

    public List<LocalDate> nights() {
        return checkInDate.datesUntil(checkOutDate).toList();
    }

    public List<NightlyPriceLine> getPriceLines() {
        return Collections.unmodifiableList(priceLines);
    }

    private void requireTransitionTo(BookingStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new ConflictException("booking " + reference + " cannot move from " + status + " to " + next);
        }
    }

    private void validateStay() {
        PreConditions.require(checkOutDate.isAfter(checkInDate), "check-out date must be after check-in date");
        PreConditions.require(roomCount >= 1, "room count must be at least 1: " + roomCount);
        PreConditions.require(guestCount >= 1, "guest count must be at least 1: " + guestCount);
        int capacity = roomCount * variant.getRoomType().getMaxOccupancy();
        PreConditions.require(guestCount <= capacity,
                guestCount + " guests exceed capacity of " + capacity + " for " + roomCount + " room(s)");
    }

    private void validateOffer() {
        PreConditions.require(variant.getRoomType().getProperty().equals(property),
                "variant does not belong to property " + property.getName());
        PreConditions.require(property.isActive(), "property is not available: " + property.getName());
        PreConditions.require(variant.isActive(), "variant is not available: " + variant.getName());
        PreConditions.require(property.accepts(bookingType),
                "property " + property.getName() + " does not accept " + bookingType + " bookings");
        PreConditions.require(policyVersion.getPolicy().equals(property.getCancellationPolicy()),
                "policy version does not belong to the property's cancellation policy");
    }

    private Money addPriceLines(List<NightlyRate> nightlyRates) {
        List<LocalDate> nights = nights();
        PreConditions.require(nightlyRates.size() == nights.size(),
                "expected " + nights.size() + " nightly rates, got " + nightlyRates.size());
        Money sum = Money.zero(nightlyRates.getFirst().baseRate().currency());
        for (int i = 0; i < nights.size(); i++) {
            NightlyRate rate = nightlyRates.get(i);
            PreConditions.require(rate.night().equals(nights.get(i)),
                    "nightly rate for " + nights.get(i) + " is missing or out of order");
            NightlyPriceLine line = new NightlyPriceLine(this, rate, roomCount);
            priceLines.add(line);
            sum = sum.plus(line.lineTotal());
        }
        return sum;
    }
}
