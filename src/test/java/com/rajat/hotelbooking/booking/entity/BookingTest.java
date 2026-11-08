package com.rajat.hotelbooking.booking.entity;

import com.rajat.hotelbooking.booking.entity.enums.BookingStatus;
import com.rajat.hotelbooking.common.entity.enums.BookingType;
import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.common.exception.DomainValidationException;
import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.Variant;
import com.rajat.hotelbooking.support.TestData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static com.rajat.hotelbooking.support.TestData.HOLD;
import static com.rajat.hotelbooking.support.TestData.NOW;
import static com.rajat.hotelbooking.support.TestData.draft;
import static com.rajat.hotelbooking.support.TestData.inr;
import static com.rajat.hotelbooking.support.TestData.pendingBooking;
import static com.rajat.hotelbooking.support.TestData.variant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 3);

    private final Variant variant = variant(5);

    @Test
    void prepaidBookingWaitsForPaymentWithAHold() {
        Booking booking = pendingBooking(variant, CHECK_IN, CHECK_OUT);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.getHoldExpiresAt()).isEqualTo(NOW.plus(HOLD));
        assertThat(booking.getReference()).matches("HB[A-HJ-NP-Z2-9]{8}");
    }

    @Test
    void totalIsTheSumOfNightlyLinesTimesRooms() {
        Booking booking = Booking.awaitingPayment(draft(variant, BookingType.PREPAID, CHECK_IN, CHECK_OUT, 2, 4),
                NOW.plus(HOLD));

        assertThat(booking.getPriceLines()).hasSize(2);
        assertThat(booking.getPriceLines().getFirst().lineTotal()).isEqualTo(inr("6000"));
        assertThat(booking.getTotal()).isEqualTo(inr("12000"));
    }

    @Test
    void payAtPropertyBookingIsConfirmedStraightAway() {
        Booking booking = Booking.confirmedWithoutPayment(
                draft(variant, BookingType.PAY_AT_PROPERTY, CHECK_IN, CHECK_OUT, 1, 2), NOW);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getHoldExpiresAt()).isNull();
    }

    @Test
    void cancelReturnsThePreviousStatus() {
        Booking booking = pendingBooking(variant, CHECK_IN, CHECK_OUT);

        assertThat(booking.cancel(NOW)).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void rejectsCancellationAfterCheckIn() {
        Booking booking = pendingBooking(variant, CHECK_IN, CHECK_OUT);
        Instant afterCheckIn = booking.checkInMoment().plusSeconds(1);

        assertThatThrownBy(() -> booking.cancel(afterCheckIn))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("after check-in");
    }

    @Test
    void rejectsCancellingTwice() {
        Booking booking = pendingBooking(variant, CHECK_IN, CHECK_OUT);
        booking.cancel(NOW);

        assertThatThrownBy(() -> booking.cancel(NOW)).isInstanceOf(ConflictException.class);
    }

    @Test
    void expiresOnlyAfterTheHoldRunsOut() {
        Booking booking = pendingBooking(variant, CHECK_IN, CHECK_OUT);

        assertThatThrownBy(() -> booking.expire(NOW)).isInstanceOf(ConflictException.class);

        booking.expire(NOW.plus(HOLD));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void confirmedBookingCannotExpire() {
        Booking booking = pendingBooking(variant, CHECK_IN, CHECK_OUT);
        booking.confirmPayment(NOW);

        assertThatThrownBy(() -> booking.expire(NOW.plus(HOLD))).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsMoreGuestsThanTheRoomsHold() {
        assertThatThrownBy(() -> Booking.awaitingPayment(
                draft(variant, BookingType.PREPAID, CHECK_IN, CHECK_OUT, 1, 3), NOW.plus(HOLD)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("exceed capacity");
    }

    @Test
    void rejectsAVariantOfAnotherProperty() {
        BookingDraft draft = draft(variant, BookingType.PREPAID, CHECK_IN, CHECK_OUT, 1, 2);
        Property other = TestData.property();
        BookingDraft mismatched = new BookingDraft(draft.customer(), other, variant,
                other.getCancellationPolicy().getCurrentVersion(), BookingType.PREPAID, CHECK_IN, CHECK_OUT, 1, 2,
                null, draft.nightlyRates(), "key", "fp");

        assertThatThrownBy(() -> Booking.awaitingPayment(mismatched, NOW.plus(HOLD)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void rejectsABookingTypeThePropertyDoesNotAccept() {
        variant.getRoomType().getProperty().changeAcceptedBookingTypes(Set.of(BookingType.PREPAID));

        assertThatThrownBy(() -> Booking.confirmedWithoutPayment(
                draft(variant, BookingType.PAY_AT_PROPERTY, CHECK_IN, CHECK_OUT, 1, 2), NOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("does not accept");
    }
}
