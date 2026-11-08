package com.rajat.hotelbooking.booking.mapper;

import com.rajat.hotelbooking.booking.dto.BookingResponse;
import com.rajat.hotelbooking.booking.dto.CancellationResponse;
import com.rajat.hotelbooking.booking.dto.CustomerSummaryResponse;
import com.rajat.hotelbooking.booking.dto.GuestResponse;
import com.rajat.hotelbooking.booking.dto.NightlyPriceResponse;
import com.rajat.hotelbooking.booking.dto.PaymentResponse;
import com.rajat.hotelbooking.booking.entity.Booking;
import com.rajat.hotelbooking.booking.entity.Customer;
import com.rajat.hotelbooking.booking.entity.GuestDetails;
import com.rajat.hotelbooking.booking.entity.NightlyPriceLine;
import com.rajat.hotelbooking.common.dto.MoneyResponse;
import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.common.mapper.MoneyMapper;
import com.rajat.hotelbooking.payment.entity.PaymentAttempt;
import com.rajat.hotelbooking.payment.entity.RefundAttempt;
import com.rajat.hotelbooking.payment.mapper.PaymentMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(uses = {MoneyMapper.class, PaymentMapper.class})
public interface BookingMapper {

    @Mapping(target = "propertyId", source = "property.id")
    @Mapping(target = "propertyName", source = "property.name")
    @Mapping(target = "variantId", source = "variant.id")
    @Mapping(target = "roomType", source = "variant.roomType.name")
    @Mapping(target = "variant", source = "variant.name")
    @Mapping(target = "checkIn", source = "checkInDate")
    @Mapping(target = "checkOut", source = "checkOutDate")
    @Mapping(target = "nights", source = ".", qualifiedByName = "nightCount")
    @Mapping(target = "rooms", source = "roomCount")
    @Mapping(target = "guests", source = "guestCount")
    @Mapping(target = "guest", source = "guestDetails")
    @Mapping(target = "nightlyPrices", source = "priceLines")
    @Mapping(target = "cancellationPolicy", source = "policyVersion.policy.name")
    @Mapping(target = "cancellationPolicyVersion", source = "policyVersion.versionNumber")
    BookingResponse toResponse(Booking booking);

    CustomerSummaryResponse toSummary(Customer customer);

    GuestResponse toResponse(GuestDetails guestDetails);

    @Mapping(target = "paymentId", source = "attempt.id")
    @Mapping(target = "bookingReference", source = "booking.reference")
    @Mapping(target = "bookingStatus", source = "booking.status")
    @Mapping(target = "status", source = "attempt.status")
    @Mapping(target = "method", source = "attempt.method")
    @Mapping(target = "amount", source = "attempt.amount")
    @Mapping(target = "paymentMethodSummary", source = "attempt", qualifiedByName = "paymentMethodSummary")
    @Mapping(target = "gatewayReference", source = "attempt.gatewayReference")
    @Mapping(target = "failureReason", source = "attempt.failureReason")
    @Mapping(target = "completedAt", source = "attempt.completedAt")
    PaymentResponse toPaymentResponse(PaymentAttempt attempt, Booking booking);

    @Mapping(target = "reference", source = "booking.reference")
    @Mapping(target = "status", source = "booking.status")
    @Mapping(target = "cancelledAt", source = "booking.cancelledAt")
    @Mapping(target = "refund", source = "refund")
    CancellationResponse toCancellationResponse(Booking booking, RefundAttempt refund);

    default NightlyPriceResponse toResponse(NightlyPriceLine line) {
        return new NightlyPriceResponse(
                line.getNight(),
                MoneyResponse.from(Money.of(line.getBaseRate(), line.getCurrency())),
                MoneyResponse.from(Money.of(line.getSurcharge(), line.getCurrency())),
                line.getRoomCount(),
                MoneyResponse.from(line.lineTotal()));
    }

    @Named("paymentMethodSummary")
    default String paymentMethodSummary(PaymentAttempt attempt) {
        return attempt.displaySummary();
    }

    @Named("nightCount")
    default int nightCount(Booking booking) {
        return booking.nights().size();
    }
}
