package com.rajat.hotelbooking.payment.entity;

import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;

import java.util.Locale;
import java.util.regex.Pattern;

public record CardDetails(String network, String last4) implements MethodDetails {

    private static final Pattern LAST4_PATTERN = Pattern.compile("[0-9]{4}");

    public CardDetails {
        network = PreConditions.requireText(network, "card network").toUpperCase(Locale.ROOT);
        last4 = PreConditions.requireText(last4, "card last 4 digits");
        PreConditions.require(LAST4_PATTERN.matcher(last4).matches(), "card last 4 must be exactly 4 digits");
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CARD;
    }

    @Override
    public String displaySummary() {
        return network + " **** " + last4;
    }
}
