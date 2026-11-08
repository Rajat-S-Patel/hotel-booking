package com.rajat.hotelbooking.payment.entity;

import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;
import com.rajat.hotelbooking.payment.entity.enums.PaymentStatus;
import com.rajat.hotelbooking.payment.entity.enums.RefundReason;
import com.rajat.hotelbooking.payment.entity.enums.RefundStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.rajat.hotelbooking.support.TestData.NOW;
import static com.rajat.hotelbooking.support.TestData.inr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentAttemptTest {

    private final PaymentAttempt attempt = PaymentAttempt.initiate(UUID.randomUUID(), inr("3000"),
            new CardDetails("visa", "4242"), "pay-1", "fp");

    @Test
    void startsInitiatedWithTheMethodFromTheDetails() {
        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(attempt.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(attempt.displaySummary()).isEqualTo("VISA **** 4242");
        assertThat(attempt.refundableAmount()).isEqualTo(inr("3000"));
    }

    @Test
    void settlesOnlyOnce() {
        attempt.markSucceeded("pg_1", NOW);

        assertThatThrownBy(() -> attempt.markFailed("late decline", NOW)).isInstanceOf(ConflictException.class);
    }

    @Test
    void onlySucceededPaymentsCanBeRefunded() {
        assertThatThrownBy(() -> attempt.requestRefund(inr("100"), RefundReason.CANCELLATION))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void refundsCannotExceedWhatWasPaid() {
        attempt.markSucceeded("pg_1", NOW);
        attempt.requestRefund(inr("2000"), RefundReason.CANCELLATION);

        assertThat(attempt.refundableAmount()).isEqualTo(inr("1000"));
        assertThatThrownBy(() -> attempt.requestRefund(inr("1500"), RefundReason.CANCELLATION))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void failedRefundGivesTheAmountBack() {
        attempt.markSucceeded("pg_1", NOW);
        RefundAttempt refund = attempt.requestRefund(inr("3000"), RefundReason.CANCELLATION);

        refund.markFailed("rejected by issuer", NOW);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
        assertThat(attempt.refundableAmount()).isEqualTo(inr("3000"));
    }

    @Test
    void upiDetailsAreNormalised() {
        UpiDetails upi = new UpiDetails("Ravi@OKAXIS");

        assertThat(upi.upiId()).isEqualTo("ravi@okaxis");
        assertThat(upi.method()).isEqualTo(PaymentMethod.UPI);
    }
}
