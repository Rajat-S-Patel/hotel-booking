package com.rajat.hotelbooking.policy.calculator;

import com.rajat.hotelbooking.policy.entity.enums.PolicyKind;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RefundCalculators {

    private final Map<PolicyKind, RefundCalculator> byKind;

    public RefundCalculators(List<RefundCalculator> calculators) {
        this.byKind = calculators.stream().collect(Collectors.toMap(RefundCalculator::kind, Function.identity()));
    }

    public RefundCalculator forKind(PolicyKind kind) {
        RefundCalculator calculator = byKind.get(kind);
        if (calculator == null) {
            throw new IllegalStateException("no refund calculator for policy kind " + kind);
        }
        return calculator;
    }
}
