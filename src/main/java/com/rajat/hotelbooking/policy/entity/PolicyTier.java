package com.rajat.hotelbooking.policy.entity;

import com.rajat.hotelbooking.common.util.PreConditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record PolicyTier(
        @Column(name = "min_hours_before_check_in", nullable = false) int minHoursBeforeCheckIn,
        @Column(name = "refund_percent", nullable = false) int refundPercent) {

    public PolicyTier {
        PreConditions.require(minHoursBeforeCheckIn >= 0,
                "min hours before check-in must not be negative: " + minHoursBeforeCheckIn);
        PreConditions.require(refundPercent >= 0 && refundPercent <= 100,
                "refund percent must be between 0 and 100: " + refundPercent);
    }
}
