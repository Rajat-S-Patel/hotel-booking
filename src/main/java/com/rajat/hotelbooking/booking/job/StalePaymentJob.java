package com.rajat.hotelbooking.booking.job;

import com.rajat.hotelbooking.booking.config.BookingProperties;
import com.rajat.hotelbooking.payment.entity.PaymentAttempt;
import com.rajat.hotelbooking.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

// Payments that never got an answer from the gateway (timeout, pending UPI) stay INITIATED and block
// new payments for the booking. After the payment timeout they are marked FAILED.
@Slf4j
@Component
@RequiredArgsConstructor
public class StalePaymentJob {

    private final PaymentService paymentService;
    private final BookingProperties properties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${hotelbooking.booking.expiry-interval:60s}",
            initialDelayString = "${hotelbooking.booking.expiry-interval:60s}")
    public void failStalePayments() {
        Instant cutoff = clock.instant().minus(properties.paymentTimeout());
        for (PaymentAttempt attempt : paymentService.findStaleAttempts(cutoff, properties.expiryBatchSize())) {
            try {
                paymentService.markTimedOut(attempt.getId());
                log.info("payment {} for booking {} timed out, marked FAILED", attempt.getId(), attempt.getBookingId());
            } catch (OptimisticLockingFailureException e) {
                log.info("payment {} changed while timing it out, leaving it as is", attempt.getId());
            }
        }
    }
}
