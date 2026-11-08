package com.rajat.hotelbooking.payment.gateway;

public record ChargeResult(Outcome outcome, String gatewayReference, String failureReason) {

    public enum Outcome {
        SUCCEEDED,
        DECLINED,
        PENDING
    }

    public static ChargeResult succeeded(String gatewayReference) {
        return new ChargeResult(Outcome.SUCCEEDED, gatewayReference, null);
    }

    public static ChargeResult declined(String reason) {
        return new ChargeResult(Outcome.DECLINED, null, reason);
    }

    public static ChargeResult pending(String gatewayReference) {
        return new ChargeResult(Outcome.PENDING, gatewayReference, null);
    }

    public boolean isPending() {
        return outcome == Outcome.PENDING;
    }
}
