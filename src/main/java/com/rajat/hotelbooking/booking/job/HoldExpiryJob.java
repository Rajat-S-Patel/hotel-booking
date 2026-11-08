package com.rajat.hotelbooking.booking.job;

import com.rajat.hotelbooking.booking.service.BookingExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpiryJob {

    private final BookingExpiryService expiryService;

    @Scheduled(fixedDelayString = "${hotelbooking.booking.expiry-interval:60s}",
            initialDelayString = "${hotelbooking.booking.expiry-interval:60s}")
    public void expireDueHolds() {
        int expired = expiryService.expireDueHolds();
        if (expired > 0) {
            log.info("expired {} unpaid booking hold(s)", expired);
        }
    }
}
