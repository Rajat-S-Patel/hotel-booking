package com.rajat.hotelbooking.payment.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardDetails.class, name = "CARD"),
        @JsonSubTypes.Type(value = UpiDetails.class, name = "UPI")
})
public sealed interface MethodDetails permits CardDetails, UpiDetails {

    PaymentMethod method();

    String displaySummary();
}
