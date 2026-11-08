package com.rajat.hotelbooking.booking.repository;

import com.rajat.hotelbooking.booking.entity.Booking;
import com.rajat.hotelbooking.booking.entity.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends Repository<Booking, UUID> {

    Booking save(Booking booking);

    Booking saveAndFlush(Booking booking);

    Optional<Booking> findById(UUID id);

    Optional<Booking> findByReference(String reference);

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    List<Booking> findByStatusAndHoldExpiresAtLessThanEqualOrderByHoldExpiresAtAsc(
            BookingStatus status, Instant now, Limit limit);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select b from Booking b where b.reference = :reference")
    Optional<Booking> findForPaymentByReference(@Param("reference") String reference);
}
