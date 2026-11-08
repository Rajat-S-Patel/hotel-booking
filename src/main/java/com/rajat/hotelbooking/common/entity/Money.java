package com.rajat.hotelbooking.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

@Embeddable
public record Money(@Column(nullable = false, precision = 12, scale = 2) BigDecimal amount,
                    @Column(nullable = false, length = 3) Currency currency) {

    public static final Currency INR = Currency.getInstance("INR");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new IllegalArgumentException("amount must have at most 2 decimal places: " + amount);
        }
        amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money inr(String amount) {
        return new Money(new BigDecimal(amount), INR);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }
    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }
    public Money times(int multiplier) {
        return new Money(amount.multiply(BigDecimal.valueOf(multiplier)), currency);
    }

    public Money percent(int percentage) {
        BigDecimal result = amount.multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        return new Money(result, currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "currency mismatch: " + currency + " vs " + other.currency);
        }
    }
}
