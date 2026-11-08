package com.rajat.hotelbooking.policy.calculator;

import com.rajat.hotelbooking.policy.entity.PolicyTier;
import com.rajat.hotelbooking.policy.entity.PolicyVersion;
import com.rajat.hotelbooking.policy.entity.enums.PolicyKind;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class TieredRefundCalculator implements RefundCalculator {

    @Override
    public PolicyKind kind() {
        return PolicyKind.TIERED;
    }

    @Override
    public int refundPercent(PolicyVersion policyVersion, Instant checkInMoment, Instant cancelledAt) {
        long hoursBefore = Duration.between(cancelledAt, checkInMoment).toHours();
        // tiers come sorted by hours, most notice first
        for (PolicyTier tier : policyVersion.getTiers()) {
            if (hoursBefore >= tier.minHoursBeforeCheckIn()) {
                return tier.refundPercent();
            }
        }
        return 0;
    }
}
