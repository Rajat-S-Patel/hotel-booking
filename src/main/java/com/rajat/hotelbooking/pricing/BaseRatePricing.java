package com.rajat.hotelbooking.pricing;

import com.rajat.hotelbooking.property.entity.Variant;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class BaseRatePricing implements PricingStrategy {

    @Override
    public List<NightlyRate> nightlyRates(Variant variant, LocalDate checkIn, LocalDate checkOut) {
        return checkIn.datesUntil(checkOut)
                .map(night -> new NightlyRate(night, variant.getRoomType().getBasePrice(), variant.getSurcharge()))
                .toList();
    }
}
