package com.rajat.hotelbooking.booking.service;

import com.rajat.hotelbooking.booking.dto.BookingResponse;
import com.rajat.hotelbooking.booking.dto.CreateBookingRequest;
import com.rajat.hotelbooking.booking.dto.CustomerRequest;
import com.rajat.hotelbooking.booking.dto.GuestRequest;
import com.rajat.hotelbooking.booking.entity.Booking;
import com.rajat.hotelbooking.booking.entity.BookingDraft;
import com.rajat.hotelbooking.booking.entity.Customer;
import com.rajat.hotelbooking.booking.entity.GuestDetails;
import com.rajat.hotelbooking.booking.flow.BookingFlowRegistry;
import com.rajat.hotelbooking.booking.mapper.BookingMapper;
import com.rajat.hotelbooking.booking.repository.BookingRepository;
import com.rajat.hotelbooking.booking.repository.CustomerRepository;
import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.common.exception.DomainValidationException;
import com.rajat.hotelbooking.common.exception.NotFoundException;
import com.rajat.hotelbooking.common.util.ContactDetails;
import com.rajat.hotelbooking.common.util.RequestFingerprint;
import com.rajat.hotelbooking.inventory.service.InventoryService;
import com.rajat.hotelbooking.pricing.PricingStrategy;
import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.Variant;
import com.rajat.hotelbooking.property.repository.VariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final VariantRepository variantRepository;
    private final InventoryService inventoryService;
    private final PricingStrategy pricingStrategy;
    private final BookingFlowRegistry flows;
    private final BookingMapper bookingMapper;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public BookingResponse create(CreateBookingRequest request, String idempotencyKey) {
        String fingerprint = fingerprint(request);
        Optional<BookingResponse> replay = findReplay(idempotencyKey, fingerprint);
        if (replay.isPresent()) {
            return replay.get();
        }
        inventoryService.ensureLedgerRows(request.variantId(), request.checkIn(), request.checkOut());
        try {
            return transaction.execute(status -> book(request, idempotencyKey, fingerprint));
        } catch (DataIntegrityViolationException e) {
            // same key raced us and won, return its booking
            return findReplay(idempotencyKey, fingerprint).orElseThrow(() -> e);
        }
    }

    public BookingResponse get(String reference) {
        return transaction.execute(status -> bookingRepository.findByReference(reference)
                .map(bookingMapper::toResponse)
                .orElseThrow(() -> NotFoundException.of("booking", reference)));
    }

    private BookingResponse book(CreateBookingRequest request, String idempotencyKey, String fingerprint) {
        Instant now = clock.instant();
        Variant variant = variantRepository.findWithRoomTypeAndPropertyById(request.variantId())
                .orElseThrow(() -> NotFoundException.of("variant", request.variantId()));
        Property property = variant.getRoomType().getProperty();
        // @FutureOrPresent on the request checks against UTC; what counts is today at the property
        LocalDate today = LocalDate.ofInstant(now, property.getTimeZone());
        if (request.checkIn().isBefore(today)) {
            throw new DomainValidationException("check-in date " + request.checkIn() + " is in the past for "
                    + property.getName() + " (today is " + today + ")");
        }
        BookingDraft draft = new BookingDraft(
                findOrCreateCustomer(request.customer()),
                property,
                variant,
                property.getCancellationPolicy().getCurrentVersion(),
                request.bookingType(),
                request.checkIn(),
                request.checkOut(),
                request.rooms(),
                request.guests(),
                toGuestDetails(request.guest()),
                pricingStrategy.nightlyRates(variant, request.checkIn(), request.checkOut()),
                idempotencyKey,
                fingerprint);
        Booking booking = flows.forType(request.bookingType()).open(draft, now);
        inventoryService.reserve(variant, booking.getCheckInDate(), booking.getCheckOutDate(), booking.getRoomCount());
        return bookingMapper.toResponse(bookingRepository.saveAndFlush(booking));
    }

    private Optional<BookingResponse> findReplay(String idempotencyKey, String fingerprint) {
        return transaction.execute(status -> bookingRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> {
                    if (!existing.getRequestFingerprint().equals(fingerprint)) {
                        throw new ConflictException("idempotency key " + idempotencyKey
                                + " was already used for a different booking request");
                    }
                    return bookingMapper.toResponse(existing);
                }));
    }

    private Customer findOrCreateCustomer(CustomerRequest request) {
        String email = ContactDetails.normaliseEmail(request.email(), "customer email");
        return customerRepository.findByEmail(email)
                .orElseGet(() -> customerRepository.save(new Customer(request.name(), email, request.phone())));
    }

    private static GuestDetails toGuestDetails(GuestRequest guest) {
        return guest == null ? null : new GuestDetails(guest.name(), guest.phone());
    }

    private static String fingerprint(CreateBookingRequest request) {
        CustomerRequest customer = request.customer();
        GuestRequest guest = request.guest();
        return RequestFingerprint.of(request.variantId(), request.checkIn(), request.checkOut(), request.rooms(),
                request.guests(), request.bookingType(),
                customer.name(), customer.email().trim().toLowerCase(Locale.ROOT), customer.phone(),
                guest == null ? null : guest.name(), guest == null ? null : guest.phone());
    }
}
