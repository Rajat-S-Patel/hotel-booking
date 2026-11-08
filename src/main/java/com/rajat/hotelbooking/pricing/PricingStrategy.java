package com.rajat.hotelbooking.pricing;

import com.rajat.hotelbooking.property.entity.Variant;

import java.time.LocalDate;
import java.util.List;

public interface PricingStrategy {

    List<NightlyRate> nightlyRates(Variant variant, LocalDate checkIn, LocalDate checkOut);
}
