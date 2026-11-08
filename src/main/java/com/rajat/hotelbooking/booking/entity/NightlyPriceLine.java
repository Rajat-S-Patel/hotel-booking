package com.rajat.hotelbooking.booking.entity;

import com.rajat.hotelbooking.common.entity.BaseEntity;
import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.pricing.NightlyRate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

@Getter
@Entity
@Immutable
@Table(name = "nightly_price_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_price_line_booking_night", columnNames = {"booking_id", "night"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NightlyPriceLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, updatable = false)
    private Booking booking;

    @Column(nullable = false, updatable = false)
    private LocalDate night;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal baseRate;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal surcharge;

    @Column(nullable = false, updatable = false)
    private int roomCount;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal lineAmount;

    @Column(nullable = false, updatable = false, length = 3)
    private Currency currency;

    NightlyPriceLine(Booking booking, NightlyRate rate, int roomCount) {
        this.booking = booking;
        this.night = rate.night();
        this.baseRate = rate.baseRate().amount();
        this.surcharge = rate.surcharge().amount();
        this.roomCount = roomCount;
        this.currency = rate.baseRate().currency();
        this.lineAmount = rate.perRoom().times(roomCount).amount();
    }

    public Money lineTotal() {
        return Money.of(lineAmount, currency);
    }
}
