package com.rajat.hotelbooking.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpiPaymentRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9.\\-_]{2,256}@[A-Za-z]{2,64}", message = "must be a valid UPI ID like name@bank")
        String upiId) {
}
