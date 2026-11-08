package com.rajat.hotelbooking.booking.flow;

import com.rajat.hotelbooking.booking.entity.Booking;
import com.rajat.hotelbooking.booking.entity.BookingDraft;
import com.rajat.hotelbooking.common.entity.enums.BookingType;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PayAtPropertyFlow implements BookingFlow {

    @Override
    public BookingType type() {
        return BookingType.PAY_AT_PROPERTY;
    }

    @Override
    public Booking open(BookingDraft draft, Instant now) {
        return Booking.confirmedWithoutPayment(draft, now);
    }
}
