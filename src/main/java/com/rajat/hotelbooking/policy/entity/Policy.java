package com.rajat.hotelbooking.policy.entity;

import com.rajat.hotelbooking.common.entity.BaseEntity;
import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.policy.entity.enums.PolicyKind;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "policy")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Policy extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "owner_id")
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "based_on_version_id")
    private PolicyVersion basedOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private PolicyVersion currentVersion;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "policy", cascade = CascadeType.PERSIST)
    @OrderBy("versionNumber ASC")
    private List<PolicyVersion> versions = new ArrayList<>();

    private Policy(String name, UUID ownerId, PolicyVersion basedOn) {
        this.name = PreConditions.requireText(name, "policy name");
        this.ownerId = ownerId;
        this.basedOn = basedOn;
    }

    public static Policy platformPolicy(String name, PolicyKind kind, List<PolicyTier> tiers) {
        Policy policy = new Policy(name, null, null);
        policy.publishVersion(kind, tiers);
        return policy;
    }

    public PolicyVersion publishVersion(PolicyKind kind, List<PolicyTier> tiers) {
        int nextNumber = currentVersion == null ? 1 : currentVersion.getVersionNumber() + 1;
        PolicyVersion version = new PolicyVersion(this, nextNumber, kind, tiers);
        versions.add(version);
        currentVersion = version;
        return version;
    }

    public boolean isPlatformPolicy() {
        return ownerId == null;
    }

    public boolean isAvailableTo(UUID propertyOwnerId) {
        return isPlatformPolicy() || ownerId.equals(propertyOwnerId);
    }
}
