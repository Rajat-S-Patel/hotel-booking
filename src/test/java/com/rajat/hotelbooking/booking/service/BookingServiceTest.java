package com.rajat.hotelbooking.booking.service;

import com.rajat.hotelbooking.booking.config.BookingProperties;
import com.rajat.hotelbooking.booking.dto.BookingResponse;
import com.rajat.hotelbooking.booking.dto.CreateBookingRequest;
import com.rajat.hotelbooking.booking.dto.CustomerRequest;
import com.rajat.hotelbooking.booking.entity.Booking;
import com.rajat.hotelbooking.booking.entity.Customer;
import com.rajat.hotelbooking.booking.entity.enums.BookingStatus;
import com.rajat.hotelbooking.booking.flow.BookingFlowRegistry;
import com.rajat.hotelbooking.booking.flow.PayAtPropertyFlow;
import com.rajat.hotelbooking.booking.flow.PrepaidFlow;
import com.rajat.hotelbooking.booking.mapper.BookingMapper;
import com.rajat.hotelbooking.booking.repository.BookingRepository;
import com.rajat.hotelbooking.booking.repository.CustomerRepository;
import com.rajat.hotelbooking.common.entity.enums.BookingType;
import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.common.exception.DomainValidationException;
import com.rajat.hotelbooking.common.exception.NotFoundException;
import com.rajat.hotelbooking.inventory.service.InventoryService;
import com.rajat.hotelbooking.pricing.BaseRatePricing;
import com.rajat.hotelbooking.property.entity.Variant;
import com.rajat.hotelbooking.property.repository.VariantRepository;
import com.rajat.hotelbooking.support.NoTransaction;
import com.rajat.hotelbooking.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.rajat.hotelbooking.support.TestData.HOLD;
import static com.rajat.hotelbooking.support.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 3);
    private static final String KEY = "book-1";

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private VariantRepository variantRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private BookingMapper bookingMapper;

    private BookingService service;
    private Variant variant;

    @BeforeEach
    void setUp() {
        service = serviceAt(TestData.CLOCK);
        variant = TestData.variant(5);
    }

    @Test
    void prepaidBookingHoldsTheRoomsWhilePaymentIsPending() {
        givenNewBooking();

        service.create(request(BookingType.PREPAID, CHECK_IN), KEY);

        Booking saved = savedBooking();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(saved.getHoldExpiresAt()).isEqualTo(NOW.plus(HOLD));
        assertThat(saved.getIdempotencyKey()).isEqualTo(KEY);
        verify(inventoryService).ensureLedgerRows(variant.getId(), CHECK_IN, CHECK_OUT);
        verify(inventoryService).reserve(variant, CHECK_IN, CHECK_OUT, 1);
    }

    @Test
    void payAtPropertyBookingIsConfirmedStraightAway() {
        givenNewBooking();

        service.create(request(BookingType.PAY_AT_PROPERTY, CHECK_IN), KEY);

        assertThat(savedBooking().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void reusesACustomerWithTheSameEmail() {
        Customer existing = TestData.customer();
        when(bookingRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(variantRepository.findWithRoomTypeAndPropertyById(variant.getId())).thenReturn(Optional.of(variant));
        when(customerRepository.findByEmail("ravi@example.com")).thenReturn(Optional.of(existing));
        when(bookingRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request(BookingType.PREPAID, CHECK_IN), KEY);

        assertThat(savedBooking().getCustomer()).isSameAs(existing);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void sameKeyAndSameBodyReturnsTheFirstBooking() {
        CreateBookingRequest request = request(BookingType.PREPAID, CHECK_IN);
        Booking existing = existingBooking(fingerprintOf(request));
        BookingResponse response = mock(BookingResponse.class);
        when(bookingRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));
        when(bookingMapper.toResponse(existing)).thenReturn(response);

        assertThat(service.create(request, KEY)).isSameAs(response);
        verify(inventoryService, never()).reserve(any(), any(), any(), any(Integer.class));
    }

    @Test
    void sameKeyWithADifferentBodyIsRejected() {
        when(bookingRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existingBooking("other-request")));

        assertThatThrownBy(() -> service.create(request(BookingType.PREPAID, CHECK_IN), KEY))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("different booking request");
    }

    @Test
    void unknownVariantIsNotFound() {
        when(bookingRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(variantRepository.findWithRoomTypeAndPropertyById(variant.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(BookingType.PREPAID, CHECK_IN), KEY))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void checkInBeforeTodayAtThePropertyIsRejected() {
        // at 20:00 UTC on 2026-11-08 it is already 2026-11-09 in Kiritimati (UTC+14)
        service = serviceAt(Clock.fixed(Instant.parse("2026-11-08T20:00:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(variant.getRoomType().getProperty(), "timeZone", ZoneId.of("Pacific/Kiritimati"));
        when(bookingRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(variantRepository.findWithRoomTypeAndPropertyById(variant.getId())).thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> service.create(request(BookingType.PREPAID, LocalDate.of(2026, 11, 8)), KEY))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("in the past");
        verify(inventoryService, never()).reserve(any(), any(), any(), any(Integer.class));
    }

    @Test
    void parallelRequestWithTheSameKeyGetsTheWinnersBooking() {
        CreateBookingRequest request = request(BookingType.PREPAID, CHECK_IN);
        Booking winner = existingBooking(fingerprintOf(request));
        BookingResponse response = mock(BookingResponse.class);
        when(bookingRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty(), Optional.of(winner));
        when(variantRepository.findWithRoomTypeAndPropertyById(variant.getId())).thenReturn(Optional.of(variant));
        when(customerRepository.findByEmail(any())).thenReturn(Optional.of(TestData.customer()));
        when(bookingRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(bookingMapper.toResponse(winner)).thenReturn(response);

        assertThat(service.create(request, KEY)).isSameAs(response);
        verify(bookingRepository, times(2)).findByIdempotencyKey(KEY);
    }

    private BookingService serviceAt(Clock clock) {
        BookingProperties properties = new BookingProperties(HOLD, Duration.ofMinutes(1), 50, Duration.ofMinutes(15));
        BookingFlowRegistry flows = new BookingFlowRegistry(List.of(new PrepaidFlow(properties), new PayAtPropertyFlow()));
        return new BookingService(bookingRepository, customerRepository, variantRepository, inventoryService,
                new BaseRatePricing(), flows, bookingMapper, clock, new NoTransaction());
    }

    private void givenNewBooking() {
        when(bookingRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(variantRepository.findWithRoomTypeAndPropertyById(variant.getId())).thenReturn(Optional.of(variant));
        when(customerRepository.findByEmail("ravi@example.com")).thenReturn(Optional.empty());
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Booking savedBooking() {
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private Booking existingBooking(String fingerprint) {
        Booking booking = TestData.pendingBooking(variant, CHECK_IN, CHECK_OUT);
        ReflectionTestUtils.setField(booking, "requestFingerprint", fingerprint);
        return booking;
    }

    private static String fingerprintOf(CreateBookingRequest request) {
        return ReflectionTestUtils.invokeMethod(BookingService.class, "fingerprint", request);
    }

    private CreateBookingRequest request(BookingType type, LocalDate checkIn) {
        return new CreateBookingRequest(variant.getId(), checkIn, checkIn.plusDays(2), 1, 2, type,
                new CustomerRequest("Ravi Shah", "Ravi@Example.com", "9811111111"), null);
    }
}
