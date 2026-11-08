package com.rajat.hotelbooking.common.util;

import com.rajat.hotelbooking.common.exception.DomainValidationException;

public final class PreConditions {
    private PreConditions() {

    }

    public static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new DomainValidationException(field + " must not be null");
        }
        return value;
    }

    public static String requireText(String value, String field) {
        requireNonNull(value, field);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new DomainValidationException(field + " must not be blank");
        }
        return trimmed;
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new DomainValidationException(message);
        }
    }
}
