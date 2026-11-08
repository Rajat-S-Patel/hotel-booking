package com.rajat.hotelbooking.policy.entity;

import com.rajat.hotelbooking.common.entity.BaseEntity;
import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.policy.entity.enums.PolicyKind;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Getter
@Entity
@Immutable
@Table(name = "policy_version",
        uniqueConstraints = @UniqueConstraint(name = "uk_policy_version_policy_number",
                columnNames = {"policy_id", "version_number"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private Policy policy;

    @Column(name = "version_number", nullable = false, updatable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, updatable = false, length = 20)
    private PolicyKind kind;

    @Getter(AccessLevel.NONE)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "policy_tier",
            joinColumns = @JoinColumn(name = "policy_version_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_policy_tier_version_hours",
                    columnNames = {"policy_version_id", "min_hours_before_check_in"}))
    @OrderBy("minHoursBeforeCheckIn DESC")
    private List<PolicyTier> tiers = new ArrayList<>();

    PolicyVersion(Policy policy, int versionNumber, PolicyKind kind, List<PolicyTier> tiers) {
        this.policy = PreConditions.requireNonNull(policy, "policy");
        PreConditions.require(versionNumber >= 1, "version number must start at 1: " + versionNumber);
        this.versionNumber = versionNumber;
        this.kind = PreConditions.requireNonNull(kind, "policy kind");
        this.tiers.addAll(validateTiers(tiers));
    }

    public List<PolicyTier> getTiers() {
        return List.copyOf(tiers);
    }

    private static List<PolicyTier> validateTiers(List<PolicyTier> tiers) {
        List<PolicyTier> sorted = new ArrayList<>(PreConditions.requireNonNull(tiers, "tiers"));
        sorted.sort(Comparator.comparingInt(PolicyTier::minHoursBeforeCheckIn).reversed());
        PreConditions.require(new HashSet<>(sorted.stream().map(PolicyTier::minHoursBeforeCheckIn).toList()).size()
                == sorted.size(), "tiers must not repeat the same hours");
        for (int i = 1; i < sorted.size(); i++) {
            PreConditions.require(sorted.get(i).refundPercent() <= sorted.get(i - 1).refundPercent(),
                    "less notice must never refund more than more notice");
        }
        return sorted;
    }
}
