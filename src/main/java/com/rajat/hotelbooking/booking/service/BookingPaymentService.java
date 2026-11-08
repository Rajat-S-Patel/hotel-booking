package com.rajat.hotelbooking.booking.service;

import com.rajat.hotelbooking.booking.dto.PaymentResponse;
import com.rajat.hotelbooking.booking.entity.Booking;
import com.rajat.hotelbooking.booking.mapper.BookingMapper;
import com.rajat.hotelbooking.booking.repository.BookingRepository;
import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.common.exception.DomainValidationException;
import com.rajat.hotelbooking.common.exception.NotFoundException;
import com.rajat.hotelbooking.common.util.RequestFingerprint;
import com.rajat.hotelbooking.payment.dto.CardPaymentRequest;
import com.rajat.hotelbooking.payment.dto.PaymentRequest;
import com.rajat.hotelbooking.payment.entity.PaymentAttempt;
import com.rajat.hotelbooking.payment.entity.RefundAttempt;
import com.rajat.hotelbooking.payment.entity.enums.RefundReason;
import com.rajat.hotelbooking.payment.gateway.ChargeResult;
import com.rajat.hotelbooking.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingPaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    private final BookingMapper bookingMapper;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public PaymentResponse pay(String reference, PaymentRequest request, String idempotencyKey) {
        String fingerprint = fingerprint(reference, request);
        Started started;
        try {
            started = transaction.execute(status -> start(reference, request, idempotencyKey, fingerprint));
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
            // another request for this booking got in first; retrying either replays it (same key) or gets a 409
            started = transaction.execute(status -> start(reference, request, idempotencyKey, fingerprint));
        }
        if (started.replay()) {
            return bookingMapper.toPaymentResponse(started.attempt(), started.booking());
        }

        // gateway is called with no transaction open
        ChargeResult result = paymentService.charge(started.attempt());
        if (result.isPending()) {
            return bookingMapper.toPaymentResponse(started.attempt(), started.booking());
        }

        Settled settled;
        try {
            settled = settle(started.attempt().getId(), started.booking().getId(), result);
        } catch (OptimisticLockingFailureException e) {
            // expiry job or a cancellation touched the booking meanwhile, read again and decide
            settled = settle(started.attempt().getId(), started.booking().getId(), result);
        }
        if (settled.refundId() != null) {
            RefundAttempt refund = paymentService.executeRefund(settled.refundId());
            log.info("payment for booking {} arrived after it was {}, refund {}",
                    reference, settled.booking().getStatus(), refund.getStatus());
        }
        return bookingMapper.toPaymentResponse(settled.attempt(), settled.booking());
    }

    private Started start(String reference, PaymentRequest request, String idempotencyKey, String fingerprint) {
        PaymentAttempt existing = paymentService.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.getRequestFingerprint().equals(fingerprint)) {
                throw new ConflictException("idempotency key " + idempotencyKey
                        + " was already used for a different payment request");
            }
            Booking booking = bookingRepository.findById(existing.getBookingId()).orElseThrow();
            return new Started(existing, booking, true);
        }

        Booking booking = bookingRepository.findForPaymentByReference(reference)
                .orElseThrow(() -> NotFoundException.of("booking", reference));
        if (!booking.isAwaitingPayment()) {
            throw new ConflictException("booking " + reference + " is " + booking.getStatus()
                    + ", no payment is due");
        }
        Money amount = Money.of(request.amount(), booking.getTotal().currency());
        if (!amount.equals(booking.getTotal())) {
            throw new DomainValidationException("amount must be the booking total " + booking.getTotal().amount());
        }
        PaymentAttempt attempt = paymentService.startAttempt(booking.getId(), amount, request, idempotencyKey,
                fingerprint);
        return new Started(attempt, booking, false);
    }

    private Settled settle(UUID attemptId, UUID bookingId, ChargeResult result) {
        return transaction.execute(status -> {
            PaymentAttempt attempt = paymentService.recordCharge(attemptId, result);
            Booking booking = bookingRepository.findById(bookingId).orElseThrow();
            UUID refundId = null;
            if (attempt.isSucceeded()) {
                if (booking.isAwaitingPayment()) {
                    // status decides, not the clock: a hold that ran out but wasn't expired yet still gets confirmed
                    booking.confirmPayment(clock.instant());
                } else {
                    RefundReason reason = switch (booking.getStatus()) {
                        case EXPIRED -> RefundReason.PAYMENT_AFTER_EXPIRY;
                        case CANCELLED -> RefundReason.PAYMENT_AFTER_CANCELLATION;
                        default -> throw new IllegalStateException(
                                "booking " + booking.getReference() + " paid twice");
                    };
                    refundId = paymentService.requestRefund(attempt, attempt.getAmount(), reason).getId();
                }
                bookingRepository.saveAndFlush(booking);
            }
            return new Settled(attempt, booking, refundId);
        });
    }

    private static String fingerprint(String reference, PaymentRequest request) {
        CardPaymentRequest card = request.card();
        return RequestFingerprint.of(reference, request.method(), request.amount().stripTrailingZeros().toPlainString(),
                card == null ? null : card.digits(),
                card == null ? null : card.expiryMonth() + "/" + card.expiryYear(),
                request.upi() == null ? null : request.upi().upiId().toLowerCase(Locale.ROOT));
    }

    private record Started(PaymentAttempt attempt, Booking booking, boolean replay) {
    }

    private record Settled(PaymentAttempt attempt, Booking booking, UUID refundId) {
    }
}
