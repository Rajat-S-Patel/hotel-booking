package com.rajat.hotelbooking.payment.service;

import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.payment.dto.PaymentRequest;
import com.rajat.hotelbooking.payment.entity.MethodDetails;
import com.rajat.hotelbooking.payment.entity.PaymentAttempt;
import com.rajat.hotelbooking.payment.entity.RefundAttempt;
import com.rajat.hotelbooking.payment.entity.enums.PaymentStatus;
import com.rajat.hotelbooking.payment.entity.enums.RefundReason;
import com.rajat.hotelbooking.payment.gateway.ChargeResult;
import com.rajat.hotelbooking.payment.gateway.PaymentGateway;
import com.rajat.hotelbooking.payment.gateway.RefundResult;
import com.rajat.hotelbooking.payment.processor.PaymentProcessorRegistry;
import com.rajat.hotelbooking.payment.repository.PaymentAttemptRepository;
import com.rajat.hotelbooking.payment.repository.RefundAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final RefundAttemptRepository refundAttemptRepository;
    private final PaymentProcessorRegistry processors;
    private final PaymentGateway gateway;
    private final Clock clock;
    private final TransactionTemplate transaction;

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<PaymentAttempt> findByIdempotencyKey(String idempotencyKey) {
        return paymentAttemptRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<PaymentAttempt> findSucceeded(UUID bookingId) {
        return paymentAttemptRepository.findFirstByBookingIdAndStatus(bookingId, PaymentStatus.SUCCEEDED);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public PaymentAttempt startAttempt(UUID bookingId, Money amount, PaymentRequest request,
                                       String idempotencyKey, String fingerprint) {
        for (PaymentAttempt previous : paymentAttemptRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)) {
            if (previous.getStatus() == PaymentStatus.INITIATED) {
                throw new ConflictException("a payment for this booking is still in progress");
            }
            if (previous.isSucceeded()) {
                throw new ConflictException("this booking is already paid");
            }
        }
        MethodDetails details = processors.forMethod(request.method()).toMethodDetails(request);
        return paymentAttemptRepository.saveAndFlush(
                PaymentAttempt.initiate(bookingId, amount, details, idempotencyKey, fingerprint));
    }

    // no transaction here on purpose. if the gateway call blows up we do not know if money moved,
    // so the attempt is left INITIATED instead of being marked failed.
    public ChargeResult charge(PaymentAttempt attempt) {
        try {
            return gateway.charge(attempt.getAmount(), attempt.getMethodDetails(), attempt.getIdempotencyKey());
        } catch (RuntimeException e) {
            log.warn("gateway charge failed for payment {}", attempt.getId(), e);
            return ChargeResult.pending(null);
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentAttempt> findStaleAttempts(Instant initiatedBefore, int limit) {
        return paymentAttemptRepository.findByStatusAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                PaymentStatus.INITIATED, initiatedBefore, Limit.of(limit));
    }

    // we never heard back from the gateway for this payment, give up on it
    @Transactional
    public void markTimedOut(UUID attemptId) {
        PaymentAttempt attempt = paymentAttemptRepository.findById(attemptId).orElseThrow();
        if (attempt.getStatus() == PaymentStatus.INITIATED) {
            attempt.markFailed("no response from payment gateway", clock.instant());
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public PaymentAttempt recordCharge(UUID attemptId, ChargeResult result) {
        PaymentAttempt attempt = paymentAttemptRepository.findById(attemptId).orElseThrow();
        switch (result.outcome()) {
            case SUCCEEDED -> attempt.markSucceeded(result.gatewayReference(), clock.instant());
            case DECLINED -> attempt.markFailed(result.failureReason(), clock.instant());
            case PENDING -> {
            }
        }
        return attempt;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public RefundAttempt requestRefund(PaymentAttempt attempt, Money amount, RefundReason reason) {
        return refundAttemptRepository.save(attempt.requestRefund(amount, reason));
    }

    public RefundAttempt executeRefund(UUID refundId) {
        RefundCall call = transaction.execute(status -> {
            RefundAttempt refund = refundAttemptRepository.findById(refundId).orElseThrow();
            return new RefundCall(refund.getPayment().getGatewayReference(), refund.getAmount());
        });
        RefundResult result;
        try {
            result = gateway.refund(call.gatewayReference(), call.amount(), refundId.toString());
        } catch (RuntimeException e) {
            log.warn("gateway refund failed for refund {}", refundId, e);
            return transaction.execute(status -> refundAttemptRepository.findById(refundId).orElseThrow());
        }
        return transaction.execute(status -> {
            RefundAttempt refund = refundAttemptRepository.findById(refundId).orElseThrow();
            if (result.succeeded()) {
                refund.markSucceeded(result.gatewayRefundReference(), clock.instant());
            } else {
                refund.markFailed(result.failureReason(), clock.instant());
            }
            return refund;
        });
    }

    private record RefundCall(String gatewayReference, Money amount) {
    }
}
