package com.rajat.hotelbooking.inventory.entity;

import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.property.entity.Variant;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.rajat.hotelbooking.support.TestData.variant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerEntryTest {

    private final Variant variant = variant(2);
    private final LedgerEntry entry = new LedgerEntry(variant, LocalDate.of(2026, 12, 1));

    @Test
    void reservesUpToTheRoomCount() {
        entry.reserve(1);
        entry.reserve(1);

        assertThat(entry.getReservedCount()).isEqualTo(2);
        assertThat(entry.available()).isZero();
    }

    @Test
    void rejectsReservingMoreThanIsLeft() {
        entry.reserve(1);

        assertThatThrownBy(() -> entry.reserve(2))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("only 1 room(s) left");
        assertThat(entry.getReservedCount()).isEqualTo(1);
    }

    @Test
    void inactiveVariantCannotBeReserved() {
        variant.deactivate();

        assertThat(entry.canReserve(1)).isFalse();
    }

    @Test
    void releaseGivesRoomsBack() {
        entry.reserve(2);
        entry.release(1);

        assertThat(entry.available()).isEqualTo(1);
    }

    @Test
    void cannotReleaseMoreThanReserved() {
        entry.reserve(1);

        assertThatThrownBy(() -> entry.release(2)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void availabilityFollowsTheVariantRoomCount() {
        entry.reserve(2);
        variant.changeRoomCount(5);

        assertThat(entry.available()).isEqualTo(3);
    }
}
