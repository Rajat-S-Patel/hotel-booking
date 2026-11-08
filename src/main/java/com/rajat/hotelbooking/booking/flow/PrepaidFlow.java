package com.rajat.hotelbooking.booking.flow;

import com.rajat.hotelbooking.booking.config.BookingProperties;
import com.rajat.hotelbooking.booking.entity.Booking;
import com.rajat.hotelbooking.booking.entity.BookingDraft;
import com.rajat.hotelbooking.common.entity.enums.BookingType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PrepaidFlow implements BookingFlow {

    private final BookingProperties properties;

    @Override
    public BookingType type() {
        return BookingType.PREPAID;
    }

    @Override
    public Booking open(BookingDraft draft, Instant now) {
        return Booking.awaitingPayment(draft, now.plus(properties.holdDuration()));
    }
}
