package com.rajat.hotelbooking.policy.calculator;

import com.rajat.hotelbooking.policy.entity.PolicyVersion;
import com.rajat.hotelbooking.policy.entity.enums.PolicyKind;

import java.time.Instant;

public interface RefundCalculator {

    PolicyKind kind();

    int refundPercent(PolicyVersion policyVersion, Instant checkInMoment, Instant cancelledAt);
}
