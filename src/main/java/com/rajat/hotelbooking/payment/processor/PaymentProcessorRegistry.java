package com.rajat.hotelbooking.payment.processor;

import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentProcessorRegistry {

    private final Map<PaymentMethod, PaymentProcessor> processors;

    public PaymentProcessorRegistry(List<PaymentProcessor> processorList) {
        this.processors = processorList.stream().collect(Collectors.toMap(PaymentProcessor::method, Function.identity()));
    }

    public PaymentProcessor forMethod(PaymentMethod method) {
        PaymentProcessor processor = processors.get(method);
        if (processor == null) {
            throw new IllegalStateException("no payment processor for " + method);
        }
        return processor;
    }
}
