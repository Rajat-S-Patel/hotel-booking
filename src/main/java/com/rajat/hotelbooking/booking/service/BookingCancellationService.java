package com.rajat.hotelbooking.booking.service;

import com.rajat.hotelbooking.booking.dto.CancellationResponse;
import com.rajat.hotelbooking.booking.entity.Booking;
import com.rajat.hotelbooking.booking.entity.enums.BookingStatus;
import com.rajat.hotelbooking.booking.mapper.BookingMapper;
import com.rajat.hotelbooking.booking.repository.BookingRepository;
import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.common.entity.enums.BookingType;
import com.rajat.hotelbooking.common.exception.NotFoundException;
import com.rajat.hotelbooking.inventory.service.InventoryService;
import com.rajat.hotelbooking.payment.entity.PaymentAttempt;
import com.rajat.hotelbooking.payment.entity.RefundAttempt;
import com.rajat.hotelbooking.payment.entity.enums.RefundReason;
import com.rajat.hotelbooking.payment.service.PaymentService;
import com.rajat.hotelbooking.policy.calculator.RefundCalculators;
import com.rajat.hotelbooking.policy.entity.PolicyVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingCancellationService {

    private final BookingRepository bookingRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final RefundCalculators refundCalculators;
    private final BookingMapper bookingMapper;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public CancellationResponse cancel(String reference) {
        Cancelled cancelled = transaction.execute(status -> cancelAndRelease(reference));
        // refund goes to the gateway after the cancellation is committed; if it fails the booking stays cancelled
        RefundAttempt refund = cancelled.refundId() == null ? null : paymentService.executeRefund(cancelled.refundId());
        return bookingMapper.toCancellationResponse(cancelled.booking(), refund);
    }

    private Cancelled cancelAndRelease(String reference) {
        Instant now = clock.instant();
        Booking booking = bookingRepository.findByReference(reference)
                .orElseThrow(() -> NotFoundException.of("booking", reference));
        BookingStatus previous = booking.cancel(now);
        if (previous.holdsInventory()) {
            inventoryService.release(booking.getVariant(), booking.getCheckInDate(), booking.getCheckOutDate(),
                    booking.getRoomCount());
        }

        UUID refundId = null;
        if (previous == BookingStatus.CONFIRMED && booking.getBookingType() == BookingType.PREPAID) {
            PaymentAttempt payment = paymentService.findSucceeded(booking.getId())
                    .orElseThrow(() -> new IllegalStateException("confirmed prepaid booking " + reference
                            + " has no successful payment"));
            Money refundAmount = refundAmount(booking, payment, now);
            if (!refundAmount.isZero()) {
                refundId = paymentService.requestRefund(payment, refundAmount, RefundReason.CANCELLATION).getId();
            }
        }
        bookingRepository.saveAndFlush(booking);
        return new Cancelled(booking, refundId);
    }

    private Money refundAmount(Booking booking, PaymentAttempt payment, Instant now) {
        PolicyVersion policy = booking.getPolicyVersion();
        int percent = refundCalculators.forKind(policy.getKind())
                .refundPercent(policy, booking.checkInMoment(), now);
        Money amount = payment.getAmount().percent(percent);
        return amount.isGreaterThan(payment.refundableAmount()) ? payment.refundableAmount() : amount;
    }

    private record Cancelled(Booking booking, UUID refundId) {
    }
}
