package com.rajat.hotelbooking.pricing;

import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.common.util.PreConditions;

import java.time.LocalDate;

public record NightlyRate(LocalDate night, Money baseRate, Money surcharge) {

    public NightlyRate {
        PreConditions.requireNonNull(night, "night");
        PreConditions.requireNonNull(baseRate, "base rate");
        PreConditions.requireNonNull(surcharge, "surcharge");
        PreConditions.require(baseRate.currency().equals(surcharge.currency()),
                "base rate and surcharge must share a currency");
    }

    public Money perRoom() {
        return baseRate.plus(surcharge);
    }
}
