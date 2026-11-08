package com.rajat.hotelbooking.payment.entity;

import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;

import java.util.Locale;

public record WalletDetails(String provider, String maskedWalletId) implements MethodDetails {

    public WalletDetails {
        provider = PreConditions.requireText(provider, "wallet provider").toUpperCase(Locale.ROOT);
        maskedWalletId = PreConditions.requireText(maskedWalletId, "masked wallet ID");
        PreConditions.require(maskedWalletId.contains("•") || maskedWalletId.contains("*"),
                "wallet ID must be masked");
    }

    public static WalletDetails masking(String provider, String walletId) {
        String id = PreConditions.requireText(walletId, "wallet ID");
        PreConditions.require(id.length() >= 4, "wallet ID too short");
        return new WalletDetails(provider, "•••• " + id.substring(id.length() - 4));
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.WALLET;
    }

    @Override
    public String displaySummary() {
        return provider + " " + maskedWalletId;
    }
}
