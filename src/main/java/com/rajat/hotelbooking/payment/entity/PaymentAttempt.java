package com.rajat.hotelbooking.payment.entity;

import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.common.entity.VersionedEntity;
import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.payment.entity.enums.PaymentMethod;
import com.rajat.hotelbooking.payment.entity.enums.PaymentStatus;
import com.rajat.hotelbooking.payment.entity.enums.RefundReason;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "payment_attempt",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_attempt_idempotency_key", columnNames = "idempotency_key"),
        indexes = @Index(name = "ix_payment_attempt_booking", columnList = "booking_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttempt extends VersionedEntity {

    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false, updatable = false, precision = 12, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, updatable = false, length = 3))
    private Money amount;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "refunded_currency", nullable = false, length = 3))
    private Money refundedAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, updatable = false, length = 20)
    private PaymentMethod method;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private MethodDetails methodDetails;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 100)
    private String gatewayReference;

    @Column(length = 255)
    private String failureReason;

    private Instant completedAt;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false, length = 64)
    private String requestFingerprint;

    private PaymentAttempt(UUID bookingId, Money amount, MethodDetails methodDetails,
                           String idempotencyKey, String requestFingerprint) {
        this.bookingId = PreConditions.requireNonNull(bookingId, "booking id");
        this.amount = PreConditions.requireNonNull(amount, "amount");
        PreConditions.require(!amount.isZero(), "payment amount must be greater than zero");
        this.methodDetails = PreConditions.requireNonNull(methodDetails, "method details");
        this.method = methodDetails.method();
        this.refundedAmount = Money.zero(amount.currency());
        this.idempotencyKey = PreConditions.requireText(idempotencyKey, "idempotency key");
        this.requestFingerprint = PreConditions.requireText(requestFingerprint, "request fingerprint");
        this.status = PaymentStatus.INITIATED;
    }

    public static PaymentAttempt initiate(UUID bookingId, Money amount, MethodDetails methodDetails,
                                          String idempotencyKey, String requestFingerprint) {
        return new PaymentAttempt(bookingId, amount, methodDetails, idempotencyKey, requestFingerprint);
    }

    public void markSucceeded(String gatewayReference, Instant now) {
        moveTo(PaymentStatus.SUCCEEDED);
        this.gatewayReference = PreConditions.requireText(gatewayReference, "gateway reference");
        this.completedAt = PreConditions.requireNonNull(now, "now");
    }

    public void markFailed(String reason, Instant now) {
        moveTo(PaymentStatus.FAILED);
        this.failureReason = PreConditions.requireText(reason, "failure reason");
        this.completedAt = PreConditions.requireNonNull(now, "now");
    }

    public boolean isSucceeded() {
        return status == PaymentStatus.SUCCEEDED;
    }

    public Money refundableAmount() {
        return amount.minus(refundedAmount);
    }

    public RefundAttempt requestRefund(Money refundAmount, RefundReason reason) {
        PreConditions.requireNonNull(refundAmount, "refund amount");
        if (!isSucceeded()) {
            throw new ConflictException("only a succeeded payment can be refunded, payment is " + status);
        }
        PreConditions.require(!refundAmount.isZero(), "refund amount must be greater than zero");
        if (refundAmount.isGreaterThan(refundableAmount())) {
            throw new ConflictException("refund " + refundAmount.amount() + " exceeds refundable "
                    + refundableAmount().amount());
        }
        this.refundedAmount = refundedAmount.plus(refundAmount);
        return new RefundAttempt(this, refundAmount, reason);
    }

    public String displaySummary() {
        return methodDetails.displaySummary();
    }

    void releaseFailedRefund(Money failedAmount) {
        this.refundedAmount = refundedAmount.minus(failedAmount);
    }

    private void moveTo(PaymentStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new ConflictException("payment cannot move from " + status + " to " + next);
        }
        this.status = next;
    }
}
