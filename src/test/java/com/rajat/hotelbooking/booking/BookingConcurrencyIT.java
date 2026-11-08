package com.rajat.hotelbooking.booking;

import com.rajat.hotelbooking.booking.dto.BookingResponse;
import com.rajat.hotelbooking.booking.dto.CreateBookingRequest;
import com.rajat.hotelbooking.booking.dto.CustomerRequest;
import com.rajat.hotelbooking.booking.dto.PaymentResponse;
import com.rajat.hotelbooking.booking.entity.enums.BookingStatus;
import com.rajat.hotelbooking.booking.service.BookingPaymentService;
import com.rajat.hotelbooking.booking.service.BookingService;
import com.rajat.hotelbooking.common.entity.enums.BookingType;
import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.inventory.entity.LedgerEntry;
import com.rajat.hotelbooking.inventory.repository.LedgerEntryRepository;
import com.rajat.hotelbooking.payment.dto.CardPaymentRequest;
import com.rajat.hotelbooking.payment.dto.PaymentRequest;
import com.rajat.hotelbooking.payment.entity.PaymentAttempt;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;
import com.rajat.hotelbooking.payment.entity.enums.PaymentStatus;
import com.rajat.hotelbooking.payment.repository.PaymentAttemptRepository;
import com.rajat.hotelbooking.policy.repository.PolicyRepository;
import com.rajat.hotelbooking.property.dto.AddressRequest;
import com.rajat.hotelbooking.property.dto.CreatePropertyRequest;
import com.rajat.hotelbooking.property.dto.CreateRoomTypeRequest;
import com.rajat.hotelbooking.property.dto.OnboardOwnerRequest;
import com.rajat.hotelbooking.property.entity.enums.OwnerKind;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import com.rajat.hotelbooking.property.service.PropertyOnboardingService;
import com.rajat.hotelbooking.reference.entity.City;
import com.rajat.hotelbooking.reference.repository.CityRepository;
import com.rajat.hotelbooking.reference.repository.LocalityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

// Runs against the real app and H2, so the row locks and version checks are the real ones.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:concurrency_it;DB_CLOSE_DELAY=-1",
        "hotelbooking.scheduling.enabled=false"})
class BookingConcurrencyIT {

    @Autowired
    private PropertyOnboardingService onboardingService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private BookingPaymentService paymentService;
    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;
    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private LocalityRepository localityRepository;
    @Autowired
    private PolicyRepository policyRepository;

    private final LocalDate day = LocalDate.now().plusDays(30);

    @Test
    void manyParallelBookingsForTheLastRoomGiveExactlyOneBooking() throws Exception {
        UUID variantId = singleRoomVariant();
        List<Callable<BookingResponse>> attempts = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CreateBookingRequest request = bookingRequest(variantId, day, day.plusDays(2), i);
            String key = UUID.randomUUID().toString();
            attempts.add(() -> bookingService.create(request, key));
        }

        List<Outcome<BookingResponse>> outcomes = runTogether(attempts);

        assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(1);
        assertThat(outcomes).filteredOn(o -> !o.succeeded())
                .allSatisfy(o -> assertThat(o.error()).isInstanceOf(ConflictException.class));
        assertThat(ledgerEntryRepository.findForStay(variantId, day, day.plusDays(2)))
                .extracting(LedgerEntry::getReservedCount).containsExactly(1, 1);
    }

    @Test
    void overlappingStaysCompeteForTheSharedNight() throws Exception {
        UUID variantId = singleRoomVariant();
        // every stay covers night day+2, but they start and end on different days
        List<Callable<BookingResponse>> attempts = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            CreateBookingRequest request = bookingRequest(variantId, day.plusDays(i % 3), day.plusDays(3 + i % 2), i);
            String key = UUID.randomUUID().toString();
            attempts.add(() -> bookingService.create(request, key));
        }

        List<Outcome<BookingResponse>> outcomes = runTogether(attempts);

        assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(1);
        assertThat(ledgerEntryRepository.findForStay(variantId, day, day.plusDays(5)))
                .allSatisfy(entry -> assertThat(entry.getReservedCount()).isLessThanOrEqualTo(1));
    }

    @Test
    void backToBackStaysOnTheLastRoomBothSucceed() throws Exception {
        UUID variantId = singleRoomVariant();
        CreateBookingRequest first = bookingRequest(variantId, day, day.plusDays(2), 1);
        CreateBookingRequest second = bookingRequest(variantId, day.plusDays(2), day.plusDays(4), 2);

        List<Outcome<BookingResponse>> outcomes = runTogether(List.of(
                () -> bookingService.create(first, UUID.randomUUID().toString()),
                () -> bookingService.create(second, UUID.randomUUID().toString())));

        assertThat(outcomes).allMatch(Outcome::succeeded);
    }

    @Test
    void parallelPaymentsForOneBookingChargeOnlyOnce() throws Exception {
        UUID variantId = singleRoomVariant();
        BookingResponse booking = bookingService.create(bookingRequest(variantId, day, day.plusDays(1), 1),
                UUID.randomUUID().toString());
        PaymentRequest card = new PaymentRequest(PaymentMethod.CARD, booking.total().amount(),
                new CardPaymentRequest("4242424242424242", 12, 2030, "123", "Ravi"), null);
        List<Callable<PaymentResponse>> attempts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String key = UUID.randomUUID().toString();
            attempts.add(() -> paymentService.pay(booking.reference(), card, key));
        }

        List<Outcome<PaymentResponse>> outcomes = runTogether(attempts);

        assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(1)
                .allSatisfy(o -> assertThat(o.result().bookingStatus()).isEqualTo(BookingStatus.CONFIRMED));
        assertThat(outcomes).filteredOn(o -> !o.succeeded()).allSatisfy(o -> assertThat(o.error())
                .isInstanceOfAny(ConflictException.class, OptimisticLockingFailureException.class));

        UUID winningPayment = outcomes.stream().filter(Outcome::succeeded).findFirst().orElseThrow().result().paymentId();
        UUID bookingId = paymentAttemptRepository.findById(winningPayment).orElseThrow().getBookingId();
        assertThat(paymentAttemptRepository.findByBookingIdOrderByCreatedAtAsc(bookingId))
                .extracting(PaymentAttempt::getStatus)
                .containsExactly(PaymentStatus.SUCCEEDED);
    }

    private UUID singleRoomVariant() {
        City city = cityRepository.findByNameIgnoreCase("Ahmedabad").orElseThrow();
        UUID localityId = localityRepository.findByCityOrderByNameAsc(city).getFirst().getId();
        UUID policyId = policyRepository.findByOwnerIdIsNullOrderByNameAsc().getFirst().getId();
        UUID ownerId = onboardingService.onboardOwner(new OnboardOwnerRequest("Race Owner", OwnerKind.INDIVIDUAL,
                "owner-" + UUID.randomUUID() + "@example.com", "9876500000")).id();
        CreateRoomTypeRequest room = new CreateRoomTypeRequest("Only Room", 2, new BigDecimal("2000"), 1, null, null);
        return onboardingService.addProperty(ownerId, new CreatePropertyRequest("Race Hotel", PropertyKind.HOTEL, 3,
                        new AddressRequest("Test Road", "380015", localityId), null, null, null, null, policyId, null,
                        List.of(room)))
                .roomTypes().getFirst().variants().getFirst().id();
    }

    private static CreateBookingRequest bookingRequest(UUID variantId, LocalDate checkIn, LocalDate checkOut, int guest) {
        return new CreateBookingRequest(variantId, checkIn, checkOut, 1, 2, BookingType.PREPAID,
                new CustomerRequest("Guest " + guest, "guest-" + UUID.randomUUID() + "@example.com", "9811111111"),
                null);
    }

    // starts every task at the same moment and collects each result or exception
    private static <T> List<Outcome<T>> runTogether(List<Callable<T>> tasks) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        List<Future<T>> futures = new ArrayList<>();
        for (Callable<T> task : tasks) {
            futures.add(pool.submit(() -> {
                start.await();
                return task.call();
            }));
        }
        start.countDown();
        List<Outcome<T>> outcomes = new ArrayList<>();
        for (Future<T> future : futures) {
            try {
                outcomes.add(new Outcome<>(future.get(), null));
            } catch (ExecutionException e) {
                outcomes.add(new Outcome<>(null, e.getCause()));
            }
        }
        pool.shutdown();
        return outcomes;
    }

    private record Outcome<T>(T result, Throwable error) {
        boolean succeeded() {
            return error == null;
        }
    }
}
