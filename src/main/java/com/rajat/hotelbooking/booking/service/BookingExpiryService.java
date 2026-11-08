package com.rajat.hotelbooking.booking.service;

import com.rajat.hotelbooking.booking.config.BookingProperties;
import com.rajat.hotelbooking.booking.entity.Booking;
import com.rajat.hotelbooking.booking.entity.enums.BookingStatus;
import com.rajat.hotelbooking.booking.repository.BookingRepository;
import com.rajat.hotelbooking.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingExpiryService {

    private final BookingRepository bookingRepository;
    private final InventoryService inventoryService;
    private final BookingProperties properties;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public int expireDueHolds() {
        Instant now = clock.instant();
        List<UUID> due = transaction.execute(status -> bookingRepository
                .findByStatusAndHoldExpiresAtLessThanEqualOrderByHoldExpiresAtAsc(
                        BookingStatus.PENDING_PAYMENT, now, Limit.of(properties.expiryBatchSize()))
                .stream()
                .map(Booking::getId)
                .toList());
        int expired = 0;
        for (UUID bookingId : due) {
            try {
                if (Boolean.TRUE.equals(transaction.execute(status -> expire(bookingId, now)))) {
                    expired++;
                }
            } catch (OptimisticLockingFailureException e) {
                log.info("booking {} changed while expiring its hold, leaving it as is", bookingId);
            }
        }
        return expired;
    }

    private boolean expire(UUID bookingId, Instant now) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        if (!booking.isHoldExpired(now)) {
            return false;
        }
        booking.expire(now);
        inventoryService.release(booking.getVariant(), booking.getCheckInDate(), booking.getCheckOutDate(),
                booking.getRoomCount());
        bookingRepository.saveAndFlush(booking);
        log.info("booking {} expired, {} room(s) released", booking.getReference(), booking.getRoomCount());
        return true;
    }
}
