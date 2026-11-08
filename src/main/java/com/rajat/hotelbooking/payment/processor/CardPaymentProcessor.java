package com.rajat.hotelbooking.payment.processor;

import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.payment.dto.CardPaymentRequest;
import com.rajat.hotelbooking.payment.dto.PaymentRequest;
import com.rajat.hotelbooking.payment.entity.CardDetails;
import com.rajat.hotelbooking.payment.entity.MethodDetails;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;

@Component
public class CardPaymentProcessor implements PaymentProcessor {

    private final Clock clock;

    public CardPaymentProcessor(Clock clock) {
        this.clock = clock;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CARD;
    }

    @Override
    public MethodDetails toMethodDetails(PaymentRequest request) {
        CardPaymentRequest card = PreConditions.requireNonNull(request.card(), "card");
        YearMonth expiry = YearMonth.of(card.expiryYear(), card.expiryMonth());
        PreConditions.require(!expiry.isBefore(YearMonth.now(clock)), "card has expired");
        return new CardDetails(network(card.digits()), card.last4());
    }

    static String network(String digits) {
        if (digits.startsWith("4")) {
            return "VISA";
        }
        if (digits.startsWith("34") || digits.startsWith("37")) {
            return "AMEX";
        }
        int firstTwo = Integer.parseInt(digits.substring(0, 2));
        int firstFour = Integer.parseInt(digits.substring(0, 4));
        if ((firstTwo >= 51 && firstTwo <= 55) || (firstFour >= 2221 && firstFour <= 2720)) {
            return "MASTERCARD";
        }
        if (firstTwo == 60 || firstTwo == 65 || firstTwo == 81 || firstTwo == 82 || digits.startsWith("508")) {
            return "RUPAY";
        }
        return "CARD";
    }
}
