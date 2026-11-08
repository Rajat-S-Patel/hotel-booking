package com.rajat.hotelbooking.payment.entity;

import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;

import java.util.Locale;
import java.util.regex.Pattern;

public record UpiDetails(String upiId) implements MethodDetails {

    private static final Pattern UPI_ID_PATTERN = Pattern.compile("[a-z0-9.\\-_]{2,256}@[a-z]{2,64}");

    public UpiDetails {
        upiId = PreConditions.requireText(upiId, "UPI ID").toLowerCase(Locale.ROOT);
        PreConditions.require(UPI_ID_PATTERN.matcher(upiId).matches(), "invalid UPI ID: " + upiId);
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.UPI;
    }

    @Override
    public String displaySummary() {
        return upiId;
    }
}
