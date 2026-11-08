package com.rajat.hotelbooking.payment.repository;

import com.rajat.hotelbooking.payment.entity.PaymentAttempt;
import com.rajat.hotelbooking.payment.entity.RefundAttempt;
import com.rajat.hotelbooking.payment.entity.enums.RefundStatus;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundAttemptRepository extends Repository<RefundAttempt, UUID> {

    RefundAttempt save(RefundAttempt refund);

    Optional<RefundAttempt> findById(UUID id);

    List<RefundAttempt> findByPaymentOrderByCreatedAtAsc(PaymentAttempt payment);

    List<RefundAttempt> findByStatusOrderByCreatedAtAsc(RefundStatus status);
}
