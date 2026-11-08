package com.rajat.hotelbooking.policy.dto;

import com.rajat.hotelbooking.policy.entity.enums.PolicyKind;

import java.util.List;
import java.util.UUID;

public record PolicyResponse(UUID id, String name, int version, PolicyKind kind, List<PolicyTierResponse> tiers) {
}
