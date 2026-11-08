package com.rajat.hotelbooking.inventory.service;

import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.inventory.entity.LedgerEntry;
import com.rajat.hotelbooking.inventory.repository.LedgerEntryRepository;
import com.rajat.hotelbooking.property.entity.RoomType;
import com.rajat.hotelbooking.property.entity.Variant;
import com.rajat.hotelbooking.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.rajat.hotelbooking.support.TestData.inr;
import static com.rajat.hotelbooking.support.TestData.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final LocalDate NIGHT_1 = LocalDate.of(2026, 12, 1);
    private static final LocalDate NIGHT_2 = LocalDate.of(2026, 12, 2);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 3);

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private LedgerRowCreator ledgerRowCreator;

    private InventoryService inventory;
    private Variant variant;

    @BeforeEach
    void setUp() {
        inventory = new InventoryService(ledgerEntryRepository, ledgerRowCreator);
        variant = TestData.variant(2);
    }

    @Test
    void createsOnlyTheMissingLedgerRows() {
        when(ledgerEntryRepository.findForStay(variant.getId(), NIGHT_1, CHECK_OUT))
                .thenReturn(List.of(new LedgerEntry(variant, NIGHT_1)));

        inventory.ensureLedgerRows(variant.getId(), NIGHT_1, CHECK_OUT);

        verify(ledgerRowCreator).insert(variant.getId(), NIGHT_2);
        verify(ledgerRowCreator, never()).insert(variant.getId(), NIGHT_1);
    }

    @Test
    void rowCreatedByAParallelBookingIsNotAnError() {
        when(ledgerEntryRepository.findForStay(variant.getId(), NIGHT_1, CHECK_OUT)).thenReturn(List.of());
        doThrow(new DataIntegrityViolationException("duplicate")).when(ledgerRowCreator).insert(variant.getId(), NIGHT_1);

        inventory.ensureLedgerRows(variant.getId(), NIGHT_1, CHECK_OUT);

        verify(ledgerRowCreator).insert(variant.getId(), NIGHT_2);
    }

    @Test
    void reservesEveryNightOfTheStay() {
        LedgerEntry first = new LedgerEntry(variant, NIGHT_1);
        LedgerEntry second = new LedgerEntry(variant, NIGHT_2);
        when(ledgerEntryRepository.lockForStay(variant.getId(), NIGHT_1, CHECK_OUT)).thenReturn(List.of(first, second));

        inventory.reserve(variant, NIGHT_1, CHECK_OUT, 2);

        assertThat(first.getReservedCount()).isEqualTo(2);
        assertThat(second.getReservedCount()).isEqualTo(2);
    }

    @Test
    void reservesNothingWhenOneNightIsFull() {
        LedgerEntry free = new LedgerEntry(variant, NIGHT_1);
        LedgerEntry full = new LedgerEntry(variant, NIGHT_2);
        full.reserve(2);
        when(ledgerEntryRepository.lockForStay(variant.getId(), NIGHT_1, CHECK_OUT)).thenReturn(List.of(free, full));

        assertThatThrownBy(() -> inventory.reserve(variant, NIGHT_1, CHECK_OUT, 1))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("sold out on " + NIGHT_2);
        assertThat(free.getReservedCount()).isZero();
    }

    @Test
    void releasesEveryNightOfTheStay() {
        LedgerEntry first = new LedgerEntry(variant, NIGHT_1);
        LedgerEntry second = new LedgerEntry(variant, NIGHT_2);
        first.reserve(1);
        second.reserve(1);
        when(ledgerEntryRepository.lockForStay(variant.getId(), NIGHT_1, CHECK_OUT)).thenReturn(List.of(first, second));

        inventory.release(variant, NIGHT_1, CHECK_OUT, 1);

        assertThat(first.getReservedCount()).isZero();
        assertThat(second.getReservedCount()).isZero();
    }

    @Test
    void missingLedgerRowsAreABug() {
        when(ledgerEntryRepository.lockForStay(variant.getId(), NIGHT_1, CHECK_OUT))
                .thenReturn(List.of(new LedgerEntry(variant, NIGHT_1)));

        assertThatThrownBy(() -> inventory.reserve(variant, NIGHT_1, CHECK_OUT, 1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void availableRoomsIsTheLowestFreeCountAcrossTheStay() {
        RoomType roomType = variant.getRoomType();
        Variant busy = withId(roomType.addVariant("Sea View", inr("1000"), 4));
        Variant untouched = withId(roomType.addVariant("Garden View", inr("500"), 3));
        Variant closed = withId(roomType.addVariant("Pool View", inr("800"), 2));
        closed.deactivate();
        LedgerEntry quietNight = new LedgerEntry(busy, NIGHT_1);
        quietNight.reserve(1);
        LedgerEntry busyNight = new LedgerEntry(busy, NIGHT_2);
        busyNight.reserve(3);
        when(ledgerEntryRepository.findForVariantsAndStay(anyCollection(), any(), any()))
                .thenReturn(List.of(quietNight, busyNight));

        Map<UUID, Integer> available = inventory.availableRooms(List.of(busy, untouched, closed), NIGHT_1, CHECK_OUT);

        assertThat(available).containsEntry(busy.getId(), 1)
                .containsEntry(untouched.getId(), 3)
                .containsEntry(closed.getId(), 0);
    }
}
