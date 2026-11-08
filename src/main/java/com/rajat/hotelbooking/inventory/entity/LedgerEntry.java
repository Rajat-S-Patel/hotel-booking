package com.rajat.hotelbooking.inventory.entity;

import com.rajat.hotelbooking.common.entity.BaseEntity;
import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.property.entity.Variant;
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

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "ledger_entry",
        uniqueConstraints = @UniqueConstraint(name = "uk_ledger_entry_variant_night", columnNames = {"variant_id", "night"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false, updatable = false)
    private Variant variant;

    @Column(nullable = false, updatable = false)
    private LocalDate night;

    @Column(nullable = false)
    private int reservedCount;

    public LedgerEntry(Variant variant, LocalDate night) {
        this.variant = PreConditions.requireNonNull(variant, "variant");
        this.night = PreConditions.requireNonNull(night, "night");
        this.reservedCount = 0;
    }

    public int available() {
        return Math.max(0, variant.getRoomCount() - reservedCount);
    }

    public boolean canReserve(int rooms) {
        return variant.isActive() && rooms <= available();
    }

    public void reserve(int rooms) {
        PreConditions.require(rooms >= 1, "rooms to reserve must be at least 1: " + rooms);
        if (!canReserve(rooms)) {
            throw new ConflictException("only " + available() + " room(s) left on " + night
                    + " for variant " + variant.getName() + ", requested " + rooms);
        }
        reservedCount += rooms;
    }

    public void release(int rooms) {
        PreConditions.require(rooms >= 1, "rooms to release must be at least 1: " + rooms);
        if (rooms > reservedCount) {
            throw new IllegalStateException("cannot release " + rooms + " room(s) on " + night
                    + ", only " + reservedCount + " reserved");
        }
        reservedCount -= rooms;
    }
}
