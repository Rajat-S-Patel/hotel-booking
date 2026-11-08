package com.rajat.hotelbooking.pricing;

import com.rajat.hotelbooking.property.entity.Variant;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

// wraps base pricing; only the base rate gets the weekend bump, not the variant surcharge
@Primary
@Component
public class WeekendSurchargePricing implements PricingStrategy {

    private static final Set<DayOfWeek> WEEKEND_NIGHTS = Set.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

    private final PricingStrategy delegate;
    private final int surchargePercent;

    public WeekendSurchargePricing(BaseRatePricing delegate, PricingProperties properties) {
        this.delegate = delegate;
        this.surchargePercent = properties.weekendSurchargePercent();
    }

    @Override
    public List<NightlyRate> nightlyRates(Variant variant, LocalDate checkIn, LocalDate checkOut) {
        return delegate.nightlyRates(variant, checkIn, checkOut).stream()
                .map(this::applyWeekendSurcharge)
                .toList();
    }

    private NightlyRate applyWeekendSurcharge(NightlyRate rate) {
        if (surchargePercent == 0 || !WEEKEND_NIGHTS.contains(rate.night().getDayOfWeek())) {
            return rate;
        }
        return new NightlyRate(rate.night(), rate.baseRate().plus(rate.baseRate().percent(surchargePercent)),
                rate.surcharge());
    }
}
