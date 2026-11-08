package com.rajat.hotelbooking.payment.gateway;

public record RefundResult(boolean succeeded, String gatewayRefundReference, String failureReason) {

    public static RefundResult succeeded(String gatewayRefundReference) {
        return new RefundResult(true, gatewayRefundReference, null);
    }

    public static RefundResult failed(String reason) {
        return new RefundResult(false, null, reason);
    }
}
