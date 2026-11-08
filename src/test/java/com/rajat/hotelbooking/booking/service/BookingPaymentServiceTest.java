package com.rajat.hotelbooking.booking.service;

import com.rajat.hotelbooking.booking.entity.Booking;
import com.rajat.hotelbooking.booking.entity.enums.BookingStatus;
import com.rajat.hotelbooking.booking.mapper.BookingMapper;
import com.rajat.hotelbooking.booking.repository.BookingRepository;
import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.common.exception.DomainValidationException;
import com.rajat.hotelbooking.payment.dto.CardPaymentRequest;
import com.rajat.hotelbooking.payment.dto.PaymentRequest;
import com.rajat.hotelbooking.payment.entity.CardDetails;
import com.rajat.hotelbooking.payment.entity.PaymentAttempt;
import com.rajat.hotelbooking.payment.entity.RefundAttempt;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;
import com.rajat.hotelbooking.payment.entity.enums.PaymentStatus;
import com.rajat.hotelbooking.payment.entity.enums.RefundReason;
import com.rajat.hotelbooking.payment.gateway.ChargeResult;
import com.rajat.hotelbooking.payment.service.PaymentService;
import com.rajat.hotelbooking.support.NoTransaction;
import com.rajat.hotelbooking.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.rajat.hotelbooking.support.TestData.HOLD;
import static com.rajat.hotelbooking.support.TestData.NOW;
import static com.rajat.hotelbooking.support.TestData.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPaymentServiceTest {

    private static final String KEY = "pay-1";

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private BookingMapper bookingMapper;

    private BookingPaymentService service;
    private Booking booking;
    private PaymentAttempt attempt;

    @BeforeEach
    void setUp() {
        service = new BookingPaymentService(bookingRepository, paymentService, bookingMapper, TestData.CLOCK,
                new NoTransaction());
        booking = TestData.pendingBooking(TestData.variant(5), LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2));
        attempt = withId(PaymentAttempt.initiate(booking.getId(), booking.getTotal(),
                new CardDetails("VISA", "4242"), KEY, "fp"));
    }

    @Test
    void successfulPaymentConfirmsTheBooking() {
        givenPaymentStarts();
        givenGatewayAnswers(ChargeResult.succeeded("pg_1"));

        service.pay(booking.getReference(), card("3000"), KEY);

        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).saveAndFlush(booking);
    }

    @Test
    void declinedPaymentLeavesTheBookingWaiting() {
        givenPaymentStarts();
        givenGatewayAnswers(ChargeResult.declined("card declined"));

        service.pay(booking.getReference(), card("3000"), KEY);

        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verify(paymentService, never()).requestRefund(any(), any(), any());
    }

    @Test
    void pendingPaymentIsNotSettled() {
        givenPaymentStarts();
        when(paymentService.charge(attempt)).thenReturn(ChargeResult.pending("pg_1"));

        service.pay(booking.getReference(), card("3000"), KEY);

        verify(paymentService, never()).recordCharge(any(), any());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void amountMustBeTheBookingTotal() {
        when(paymentService.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(bookingRepository.findForPaymentByReference(booking.getReference())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.pay(booking.getReference(), card("2999.99"), KEY))
                .isInstanceOf(DomainValidationException.class);
        verify(paymentService, never()).startAttempt(any(), any(), any(), any(), any());
    }

    @Test
    void confirmedBookingCannotBePaidAgain() {
        booking.confirmPayment(NOW);
        when(paymentService.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(bookingRepository.findForPaymentByReference(booking.getReference())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.pay(booking.getReference(), card("3000"), KEY))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no payment is due");
    }

    @Test
    void sameKeyAndSameBodyReplaysWithoutCharging() {
        PaymentRequest request = card("3000");
        ReflectionTestUtils.setField(attempt, "requestFingerprint", fingerprintOf(booking.getReference(), request));
        when(paymentService.findByIdempotencyKey(KEY)).thenReturn(Optional.of(attempt));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.pay(booking.getReference(), request, KEY);

        verify(bookingMapper).toPaymentResponse(attempt, booking);
        verify(paymentService, never()).charge(any());
    }

    @Test
    void sameKeyWithADifferentBodyIsRejected() {
        when(paymentService.findByIdempotencyKey(KEY)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.pay(booking.getReference(), card("3000"), KEY))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("different payment request");
    }

    @Test
    void paymentThatLandsAfterTheHoldExpiredIsRefunded() {
        givenPaymentStarts();
        when(paymentService.charge(attempt)).thenAnswer(invocation -> {
            booking.expire(NOW.plus(HOLD));
            return ChargeResult.succeeded("pg_1");
        });
        whenRecordChargeSucceeds();
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        AtomicReference<RefundAttempt> refund = new AtomicReference<>();
        when(paymentService.requestRefund(attempt, attempt.getAmount(), RefundReason.PAYMENT_AFTER_EXPIRY))
                .thenAnswer(invocation -> {
                    refund.set(withId(attempt.requestRefund(attempt.getAmount(), RefundReason.PAYMENT_AFTER_EXPIRY)));
                    return refund.get();
                });
        when(paymentService.executeRefund(any())).thenAnswer(invocation -> refund.get());

        service.pay(booking.getReference(), card("3000"), KEY);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verify(paymentService).executeRefund(refund.get().getId());
    }

    @Test
    void retriesTheSettlementOnceAfterAConcurrentUpdate() {
        givenPaymentStarts();
        when(paymentService.charge(attempt)).thenReturn(ChargeResult.succeeded("pg_1"));
        whenRecordChargeSucceeds();
        when(bookingRepository.findById(booking.getId()))
                .thenThrow(new OptimisticLockingFailureException("booking changed"))
                .thenReturn(Optional.of(booking));

        service.pay(booking.getReference(), card("3000"), KEY);

        verify(paymentService, times(2)).recordCharge(any(), any());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    private void givenPaymentStarts() {
        when(paymentService.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(bookingRepository.findForPaymentByReference(booking.getReference())).thenReturn(Optional.of(booking));
        when(paymentService.startAttempt(eq(booking.getId()), eq(booking.getTotal()), any(), eq(KEY), anyString()))
                .thenReturn(attempt);
    }

    private void givenGatewayAnswers(ChargeResult result) {
        when(paymentService.charge(attempt)).thenReturn(result);
        when(paymentService.recordCharge(attempt.getId(), result)).thenAnswer(invocation -> {
            if (result.outcome() == ChargeResult.Outcome.SUCCEEDED) {
                attempt.markSucceeded(result.gatewayReference(), NOW);
            } else {
                attempt.markFailed(result.failureReason(), NOW);
            }
            return attempt;
        });
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
    }

    // the real transaction would roll back on a retry, so only settle an attempt that is still open
    private void whenRecordChargeSucceeds() {
        when(paymentService.recordCharge(eq(attempt.getId()), any())).thenAnswer(invocation -> {
            if (attempt.getStatus() == PaymentStatus.INITIATED) {
                attempt.markSucceeded("pg_1", NOW);
            }
            return attempt;
        });
    }

    private static PaymentRequest card(String amount) {
        return new PaymentRequest(PaymentMethod.CARD, new BigDecimal(amount),
                new CardPaymentRequest("4242424242424242", 12, 2030, "123", "Ravi"), null);
    }

    private static String fingerprintOf(String reference, PaymentRequest request) {
        return ReflectionTestUtils.invokeMethod(BookingPaymentService.class, "fingerprint", reference, request);
    }
}
