package com.rajat.hotelbooking.payment.processor;

import com.rajat.hotelbooking.payment.dto.PaymentRequest;
import com.rajat.hotelbooking.payment.entity.MethodDetails;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;

public interface PaymentProcessor {

    PaymentMethod method();

    MethodDetails toMethodDetails(PaymentRequest request);
}
