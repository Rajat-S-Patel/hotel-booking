package com.rajat.hotelbooking.payment.processor;

import com.rajat.hotelbooking.common.exception.DomainValidationException;
import com.rajat.hotelbooking.payment.dto.CardPaymentRequest;
import com.rajat.hotelbooking.payment.dto.PaymentRequest;
import com.rajat.hotelbooking.payment.entity.CardDetails;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;
import com.rajat.hotelbooking.support.TestData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardPaymentProcessorTest {

    private final CardPaymentProcessor processor = new CardPaymentProcessor(TestData.CLOCK);

    @Test
    void keepsOnlyNetworkAndLastFour() {
        CardDetails details = (CardDetails) processor.toMethodDetails(card("4242 4242 4242 4242", 12, 2030));

        assertThat(details).isEqualTo(new CardDetails("VISA", "4242"));
    }

    @Test
    void detectsTheNetworkFromTheLeadingDigits() {
        assertThat(CardPaymentProcessor.network("4111111111111111")).isEqualTo("VISA");
        assertThat(CardPaymentProcessor.network("5500000000000004")).isEqualTo("MASTERCARD");
        assertThat(CardPaymentProcessor.network("2221000000000009")).isEqualTo("MASTERCARD");
        assertThat(CardPaymentProcessor.network("378282246310005")).isEqualTo("AMEX");
        assertThat(CardPaymentProcessor.network("6521000000000000")).isEqualTo("RUPAY");
        assertThat(CardPaymentProcessor.network("9999000000000000")).isEqualTo("CARD");
    }

    @Test
    void rejectsAnExpiredCard() {
        assertThatThrownBy(() -> processor.toMethodDetails(card("4242424242424242", 10, 2026)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void cardExpiringThisMonthIsStillValid() {
        assertThat(processor.toMethodDetails(card("4242424242424242", 11, 2026))).isNotNull();
    }

    private static PaymentRequest card(String number, int month, int year) {
        return new PaymentRequest(PaymentMethod.CARD, BigDecimal.TEN,
                new CardPaymentRequest(number, month, year, "123", "Ravi"), null);
    }
}
