package com.rajat.hotelbooking.booking.entity;

import java.security.SecureRandom;

final class BookingReference {

    private static final String PREFIX = "HB";
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int RANDOM_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private BookingReference() {
    }

    static String generate() {
        StringBuilder reference = new StringBuilder(PREFIX);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            reference.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return reference.toString();
    }
}
