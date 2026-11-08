package com.rajat.hotelbooking.common.dto;

import com.rajat.hotelbooking.common.entity.Money;

import java.math.BigDecimal;

public record MoneyResponse(BigDecimal amount, String currency) {

    public static MoneyResponse from(Money money) {
        return money == null ? null : new MoneyResponse(money.amount(), money.currency().getCurrencyCode());
    }
}
