package com.rajat.hotelbooking.payment.repository;

import com.rajat.hotelbooking.payment.entity.PaymentAttempt;
import com.rajat.hotelbooking.payment.entity.enums.PaymentStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends Repository<PaymentAttempt, UUID> {

    PaymentAttempt save(PaymentAttempt attempt);

    PaymentAttempt saveAndFlush(PaymentAttempt attempt);

    Optional<PaymentAttempt> findById(UUID id);

    Optional<PaymentAttempt> findByIdempotencyKey(String idempotencyKey);

    List<PaymentAttempt> findByBookingIdOrderByCreatedAtAsc(UUID bookingId);

    Optional<PaymentAttempt> findFirstByBookingIdAndStatus(UUID bookingId, PaymentStatus status);

    List<PaymentAttempt> findByStatusAndCreatedAtLessThanEqualOrderByCreatedAtAsc(PaymentStatus status,
                                                                                 Instant cutoff, Limit limit);
}
