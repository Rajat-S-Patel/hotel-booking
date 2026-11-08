package com.rajat.hotelbooking.payment.service;

import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.payment.dto.CardPaymentRequest;
import com.rajat.hotelbooking.payment.dto.PaymentRequest;
import com.rajat.hotelbooking.payment.entity.CardDetails;
import com.rajat.hotelbooking.payment.entity.PaymentAttempt;
import com.rajat.hotelbooking.payment.entity.RefundAttempt;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;
import com.rajat.hotelbooking.payment.entity.enums.PaymentStatus;
import com.rajat.hotelbooking.payment.entity.enums.RefundReason;
import com.rajat.hotelbooking.payment.entity.enums.RefundStatus;
import com.rajat.hotelbooking.payment.gateway.ChargeResult;
import com.rajat.hotelbooking.payment.gateway.PaymentGateway;
import com.rajat.hotelbooking.payment.gateway.RefundResult;
import com.rajat.hotelbooking.payment.processor.CardPaymentProcessor;
import com.rajat.hotelbooking.payment.processor.PaymentProcessorRegistry;
import com.rajat.hotelbooking.payment.processor.UpiPaymentProcessor;
import com.rajat.hotelbooking.payment.repository.PaymentAttemptRepository;
import com.rajat.hotelbooking.payment.repository.RefundAttemptRepository;
import com.rajat.hotelbooking.support.NoTransaction;
import com.rajat.hotelbooking.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.rajat.hotelbooking.support.TestData.NOW;
import static com.rajat.hotelbooking.support.TestData.inr;
import static com.rajat.hotelbooking.support.TestData.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private RefundAttemptRepository refundAttemptRepository;
    @Mock
    private PaymentGateway gateway;

    private PaymentService service;
    private final UUID bookingId = UUID.randomUUID();
    private PaymentAttempt attempt;

    @BeforeEach
    void setUp() {
        PaymentProcessorRegistry processors = new PaymentProcessorRegistry(
                List.of(new CardPaymentProcessor(TestData.CLOCK), new UpiPaymentProcessor()));
        service = new PaymentService(paymentAttemptRepository, refundAttemptRepository, processors, gateway,
                TestData.CLOCK, new NoTransaction());
        attempt = withId(PaymentAttempt.initiate(bookingId, inr("3000"), new CardDetails("VISA", "4242"), "pay-1", "fp"));
    }

    @Test
    void startsAnAttemptWithMaskedCardDetails() {
        when(paymentAttemptRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)).thenReturn(List.of());
        when(paymentAttemptRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentAttempt started = service.startAttempt(bookingId, inr("3000"), cardRequest(), "pay-2", "fp");

        assertThat(started.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(started.getMethodDetails()).isEqualTo(new CardDetails("VISA", "4242"));
    }

    @Test
    void anotherPaymentInProgressBlocksANewOne() {
        when(paymentAttemptRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)).thenReturn(List.of(attempt));

        assertThatThrownBy(() -> service.startAttempt(bookingId, inr("3000"), cardRequest(), "pay-2", "fp"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("in progress");
    }

    @Test
    void paidBookingBlocksANewPayment() {
        attempt.markSucceeded("pg_1", NOW);
        when(paymentAttemptRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)).thenReturn(List.of(attempt));

        assertThatThrownBy(() -> service.startAttempt(bookingId, inr("3000"), cardRequest(), "pay-2", "fp"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void failedEarlierAttemptAllowsARetry() {
        attempt.markFailed("declined", NOW);
        when(paymentAttemptRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)).thenReturn(List.of(attempt));
        when(paymentAttemptRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.startAttempt(bookingId, inr("3000"), cardRequest(), "pay-2", "fp")).isNotSameAs(attempt);
    }

    @Test
    void gatewayErrorLeavesTheOutcomeUnknown() {
        when(gateway.charge(any(), any(), any())).thenThrow(new RuntimeException("timeout"));

        assertThat(service.charge(attempt).isPending()).isTrue();
    }

    @Test
    void recordsTheGatewayOutcome() {
        when(paymentAttemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));

        service.recordCharge(attempt.getId(), ChargeResult.pending("pg_1"));
        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.INITIATED);

        service.recordCharge(attempt.getId(), ChargeResult.declined("insufficient funds"));
        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(attempt.getFailureReason()).isEqualTo("insufficient funds");
    }

    @Test
    void successfulRefundIsRecorded() {
        RefundAttempt refund = refundOf("1000");
        when(gateway.refund("pg_1", inr("1000"), refund.getId().toString())).thenReturn(RefundResult.succeeded("rf_1"));

        assertThat(service.executeRefund(refund.getId()).getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
        assertThat(attempt.refundableAmount()).isEqualTo(inr("2000"));
    }

    @Test
    void rejectedRefundGivesTheAmountBack() {
        RefundAttempt refund = refundOf("1000");
        when(gateway.refund(any(), any(), any())).thenReturn(RefundResult.failed("rejected"));

        assertThat(service.executeRefund(refund.getId()).getStatus()).isEqualTo(RefundStatus.FAILED);
        assertThat(attempt.refundableAmount()).isEqualTo(inr("3000"));
    }

    @Test
    void gatewayErrorOnRefundLeavesItOpen() {
        RefundAttempt refund = refundOf("1000");
        when(gateway.refund(any(), any(), any())).thenThrow(new RuntimeException("timeout"));

        assertThat(service.executeRefund(refund.getId()).getStatus()).isEqualTo(RefundStatus.INITIATED);
    }

    @Test
    void timedOutPaymentIsMarkedFailed() {
        when(paymentAttemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));

        service.markTimedOut(attempt.getId());

        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(attempt.getFailureReason()).isEqualTo("no response from payment gateway");
    }

    @Test
    void timeoutDoesNotTouchASettledPayment() {
        attempt.markSucceeded("pg_1", NOW);
        when(paymentAttemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));

        service.markTimedOut(attempt.getId());

        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void staleAttemptsAreTheOpenOnesBeforeTheCutoff() {
        Instant cutoff = NOW.minusSeconds(900);

        service.findStaleAttempts(cutoff, 50);

        verify(paymentAttemptRepository).findByStatusAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                PaymentStatus.INITIATED, cutoff, Limit.of(50));
    }

    private RefundAttempt refundOf(String amount) {
        attempt.markSucceeded("pg_1", NOW);
        RefundAttempt refund = withId(attempt.requestRefund(inr(amount), RefundReason.CANCELLATION));
        when(refundAttemptRepository.findById(refund.getId())).thenReturn(Optional.of(refund));
        return refund;
    }

    private static PaymentRequest cardRequest() {
        return new PaymentRequest(PaymentMethod.CARD, new BigDecimal("3000"),
                new CardPaymentRequest("4242424242424242", 12, 2030, "123", "Ravi"), null);
    }
}
