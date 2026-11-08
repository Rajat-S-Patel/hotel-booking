package com.rajat.hotelbooking.payment.processor;

import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.payment.dto.PaymentRequest;
import com.rajat.hotelbooking.payment.entity.MethodDetails;
import com.rajat.hotelbooking.payment.entity.UpiDetails;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class UpiPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.UPI;
    }

    @Override
    public MethodDetails toMethodDetails(PaymentRequest request) {
        return new UpiDetails(PreConditions.requireNonNull(request.upi(), "upi").upiId());
    }
}
