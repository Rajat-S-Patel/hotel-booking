package com.rajat.hotelbooking.booking.controller;

import com.rajat.hotelbooking.booking.dto.BookingResponse;
import com.rajat.hotelbooking.booking.dto.CancellationResponse;
import com.rajat.hotelbooking.booking.dto.CreateBookingRequest;
import com.rajat.hotelbooking.booking.dto.PaymentResponse;
import com.rajat.hotelbooking.booking.service.BookingCancellationService;
import com.rajat.hotelbooking.booking.service.BookingPaymentService;
import com.rajat.hotelbooking.booking.service.BookingService;
import com.rajat.hotelbooking.common.dto.ValidationPatterns;
import com.rajat.hotelbooking.common.web.ApiPaths;
import com.rajat.hotelbooking.payment.dto.PaymentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Bookings", description = "Book a room, pay for it and cancel it")
@RestController
@RequestMapping(ApiPaths.V1 + "/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingPaymentService paymentService;
    private final BookingCancellationService cancellationService;

    @Operation(summary = "Create a booking; prepaid bookings hold the rooms for 15 minutes pending payment")
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader(ApiPaths.IDEMPOTENCY_KEY_HEADER) @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse booking = bookingService.create(request, idempotencyKey);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{reference}").buildAndExpand(booking.reference()).toUri();
        return ResponseEntity.created(location).body(booking);
    }

    @Operation(summary = "Get a booking with its status and nightly price breakdown")
    @GetMapping("/{reference}")
    public BookingResponse getBooking(@PathVariable @Pattern(regexp = ValidationPatterns.BOOKING_REFERENCE) String reference) {
        return bookingService.get(reference);
    }

    @Operation(summary = "Pay for a booking by card or UPI; returns 402 when the payment is declined")
    @PostMapping("/{reference}/payments")
    public ResponseEntity<PaymentResponse> pay(
            @PathVariable @Pattern(regexp = ValidationPatterns.BOOKING_REFERENCE) String reference,
            @RequestHeader(ApiPaths.IDEMPOTENCY_KEY_HEADER) @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse payment = paymentService.pay(reference, request, idempotencyKey);
        HttpStatus status = switch (payment.status()) {
            case SUCCEEDED -> HttpStatus.OK;
            case INITIATED -> HttpStatus.ACCEPTED;
            case FAILED -> HttpStatus.PAYMENT_REQUIRED;
        };
        return ResponseEntity.status(status).body(payment);
    }

    @Operation(summary = "Cancel a booking; refunds according to the cancellation policy captured at booking time")
    @PostMapping("/{reference}/cancellation")
    public CancellationResponse cancel(@PathVariable @Pattern(regexp = ValidationPatterns.BOOKING_REFERENCE) String reference) {
        return cancellationService.cancel(reference);
    }
}
