package com.rajat.hotelbooking.payment.dto;

import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.stream.Stream;

public record PaymentRequest(
        @NotNull PaymentMethod method,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @Valid CardPaymentRequest card,
        @Valid UpiPaymentRequest upi) {

    @AssertTrue(message = "provide exactly one of card or upi, matching the payment method")
    public boolean isInstrumentMatchingMethod() {
        long provided = Stream.of(card, upi).filter(instrument -> instrument != null).count();
        if (method == null || provided != 1) {
            return method == null && provided <= 1;
        }
        return switch (method) {
            case CARD -> card != null;
            case UPI -> upi != null;
        };
    }
}
