package com.rajat.hotelbooking.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class RequestFingerprint {

    private RequestFingerprint() {
    }

    public static String of(Object... parts) {
        StringBuilder canonical = new StringBuilder();
        for (Object part : parts) {
            String text = Objects.toString(part, "");
            canonical.append(text.length()).append(':').append(text).append('|');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
