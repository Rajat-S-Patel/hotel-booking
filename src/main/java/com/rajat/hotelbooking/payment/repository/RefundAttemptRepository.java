package com.rajat.hotelbooking.payment.repository;

import com.rajat.hotelbooking.payment.entity.RefundAttempt;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface RefundAttemptRepository extends Repository<RefundAttempt, UUID> {

    RefundAttempt save(RefundAttempt refund);

    Optional<RefundAttempt> findById(UUID id);
}
