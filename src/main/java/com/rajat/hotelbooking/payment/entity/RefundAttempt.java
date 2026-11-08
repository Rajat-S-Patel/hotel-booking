package com.rajat.hotelbooking.payment.entity;

import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.common.entity.VersionedEntity;
import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.payment.entity.enums.RefundReason;
import com.rajat.hotelbooking.payment.entity.enums.RefundStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Entity
@Table(name = "refund_attempt")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundAttempt extends VersionedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_attempt_id", nullable = false, updatable = false)
    private PaymentAttempt payment;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false, updatable = false, precision = 12, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, updatable = false, length = 3))
    private Money amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, updatable = false, length = 40)
    private RefundReason reason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    @Column(length = 100)
    private String gatewayRefundReference;

    @Column(length = 255)
    private String failureReason;

    private Instant completedAt;

    RefundAttempt(PaymentAttempt payment, Money amount, RefundReason reason) {
        this.payment = PreConditions.requireNonNull(payment, "payment");
        this.amount = PreConditions.requireNonNull(amount, "amount");
        this.reason = PreConditions.requireNonNull(reason, "refund reason");
        this.status = RefundStatus.INITIATED;
    }

    public void markSucceeded(String gatewayRefundReference, Instant now) {
        moveTo(RefundStatus.SUCCEEDED);
        this.gatewayRefundReference = PreConditions.requireText(gatewayRefundReference, "gateway refund reference");
        this.completedAt = PreConditions.requireNonNull(now, "now");
    }

    public void markFailed(String reason, Instant now) {
        moveTo(RefundStatus.FAILED);
        this.failureReason = PreConditions.requireText(reason, "failure reason");
        this.completedAt = PreConditions.requireNonNull(now, "now");
        payment.releaseFailedRefund(amount);
    }

    private void moveTo(RefundStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new ConflictException("refund cannot move from " + status + " to " + next);
        }
        this.status = next;
    }
}
