package com.rajat.hotelbooking.booking.flow;

import com.rajat.hotelbooking.booking.entity.Booking;
import com.rajat.hotelbooking.booking.entity.BookingDraft;
import com.rajat.hotelbooking.common.entity.enums.BookingType;

import java.time.Instant;

public interface BookingFlow {

    BookingType type();

    Booking open(BookingDraft draft, Instant now);
}
