package com.rajat.hotelbooking.payment.gateway;

import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.payment.entity.MethodDetails;

public interface PaymentGateway {

    ChargeResult charge(Money amount, MethodDetails methodDetails, String idempotencyKey);

    RefundResult refund(String gatewayReference, Money amount, String idempotencyKey);
}
