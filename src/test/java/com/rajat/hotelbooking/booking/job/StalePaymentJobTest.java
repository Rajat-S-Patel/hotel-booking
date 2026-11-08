package com.rajat.hotelbooking.booking.job;

import com.rajat.hotelbooking.booking.config.BookingProperties;
import com.rajat.hotelbooking.payment.entity.CardDetails;
import com.rajat.hotelbooking.payment.entity.PaymentAttempt;
import com.rajat.hotelbooking.payment.service.PaymentService;
import com.rajat.hotelbooking.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.rajat.hotelbooking.support.TestData.NOW;
import static com.rajat.hotelbooking.support.TestData.inr;
import static com.rajat.hotelbooking.support.TestData.withId;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StalePaymentJobTest {

    @Mock
    private PaymentService paymentService;

    @Test
    void failsEveryStalePaymentEvenIfOneConflicts() {
        BookingProperties properties = new BookingProperties(Duration.ofMinutes(15), Duration.ofMinutes(1), 50,
                Duration.ofMinutes(15));
        StalePaymentJob job = new StalePaymentJob(paymentService, properties, TestData.CLOCK);
        PaymentAttempt first = attempt();
        PaymentAttempt second = attempt();
        when(paymentService.findStaleAttempts(NOW.minus(Duration.ofMinutes(15)), 50)).thenReturn(List.of(first, second));
        doThrow(new OptimisticLockingFailureException("settled meanwhile")).when(paymentService).markTimedOut(first.getId());

        job.failStalePayments();

        verify(paymentService).markTimedOut(second.getId());
    }

    private static PaymentAttempt attempt() {
        return withId(PaymentAttempt.initiate(UUID.randomUUID(), inr("3000"), new CardDetails("VISA", "4242"),
                UUID.randomUUID().toString(), "fp"));
    }
}
