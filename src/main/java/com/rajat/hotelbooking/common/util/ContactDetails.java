package com.rajat.hotelbooking.common.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ContactDetails {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?[0-9]{10,15}");
    private static final int MAX_EMAIL_LENGTH = 200;

    private ContactDetails() {
    }

    public static String normaliseEmail(String email, String field) {
        String trimmed = PreConditions.requireText(email, field).toLowerCase(Locale.ROOT);
        PreConditions.require(trimmed.length() <= MAX_EMAIL_LENGTH,
                field + " must be at most " + MAX_EMAIL_LENGTH + " characters");
        PreConditions.require(EMAIL_PATTERN.matcher(trimmed).matches(), "invalid " + field + ": " + trimmed);
        return trimmed;
    }

    public static String normalisePhone(String phone, String field) {
        String compact = PreConditions.requireText(phone, field).replaceAll("[\\s-]", "");
        PreConditions.require(PHONE_PATTERN.matcher(compact).matches(), "invalid " + field + ": " + phone);
        return compact;
    }
}
