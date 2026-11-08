package com.rajat.hotelbooking.payment.gateway;

import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.payment.entity.CardDetails;
import com.rajat.hotelbooking.payment.entity.MethodDetails;
import com.rajat.hotelbooking.payment.entity.UpiDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Test instruments:
//   card ...0002 / UPI fail@bank     -> declined
//   card ...0119 / UPI pending@bank  -> no answer yet, stays INITIATED
//   card ...0341                     -> charge ok, refund rejected
//   anything else                    -> success
@Slf4j
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final String REFUND_REJECTING_PREFIX = "pg_nr_";

    @Override
    public ChargeResult charge(Money amount, MethodDetails methodDetails, String idempotencyKey) {
        ChargeResult result = switch (methodDetails) {
            case CardDetails card -> chargeCard(card);
            case UpiDetails upi -> chargeUpi(upi);
        };
        log.info("mock gateway charge {} {} via {}: {}", amount.amount(), amount.currency(),
                methodDetails.displaySummary(), result.outcome());
        return result;
    }

    @Override
    public RefundResult refund(String gatewayReference, Money amount, String idempotencyKey) {
        if (gatewayReference.startsWith(REFUND_REJECTING_PREFIX)) {
            return RefundResult.failed("refund rejected by issuer");
        }
        return RefundResult.succeeded("rf_" + UUID.randomUUID());
    }

    private static ChargeResult chargeCard(CardDetails card) {
        return switch (card.last4()) {
            case "0002" -> ChargeResult.declined("card declined by issuer");
            case "0119" -> ChargeResult.pending(newReference("pg_"));
            case "0341" -> ChargeResult.succeeded(newReference(REFUND_REJECTING_PREFIX));
            default -> ChargeResult.succeeded(newReference("pg_"));
        };
    }

    private static ChargeResult chargeUpi(UpiDetails upi) {
        String handle = upi.upiId().substring(0, upi.upiId().indexOf('@'));
        return switch (handle) {
            case "fail" -> ChargeResult.declined("UPI payment declined by payer bank");
            case "pending" -> ChargeResult.pending(newReference("pg_"));
            default -> ChargeResult.succeeded(newReference("pg_"));
        };
    }

    private static String newReference(String prefix) {
        return prefix + UUID.randomUUID();
    }
}
