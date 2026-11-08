package com.rajat.hotelbooking.common.dto;

public final class ValidationPatterns {

    public static final String PHONE = "\\+?[0-9][0-9 -]{9,19}";
    public static final String PHONE_MESSAGE = "must be a phone number of 10 to 15 digits";
    public static final String BOOKING_REFERENCE = "HB[A-HJ-NP-Z2-9]{8}";

    private ValidationPatterns() {
    }
}
