package com.rajat.hotelbooking.payment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.CreditCardNumber;

public record CardPaymentRequest(
        @NotBlank @CreditCardNumber(ignoreNonDigitCharacters = true) String number,
        @NotNull @Min(1) @Max(12) Integer expiryMonth,
        @NotNull @Min(2000) @Max(2100) Integer expiryYear,
        @NotBlank @Pattern(regexp = "[0-9]{3,4}", message = "must be 3 or 4 digits") String cvv,
        @NotBlank @Size(max = 100) String holderName) {

    public String digits() {
        return number == null ? null : number.replaceAll("[^0-9]", "");
    }

    public String last4() {
        String digits = digits();
        return digits == null || digits.length() < 4 ? null : digits.substring(digits.length() - 4);
    }

    @Override
    public String toString() {
        return "CardPaymentRequest[number=•••• " + last4() + ", expiry=" + expiryMonth + "/" + expiryYear
                + ", cvv=***, holderName=" + holderName + "]";
    }
}
