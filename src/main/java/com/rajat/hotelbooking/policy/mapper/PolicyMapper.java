package com.rajat.hotelbooking.policy.mapper;

import com.rajat.hotelbooking.policy.dto.PolicyResponse;
import com.rajat.hotelbooking.policy.dto.PolicyTierResponse;
import com.rajat.hotelbooking.policy.entity.Policy;
import com.rajat.hotelbooking.policy.entity.PolicyTier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface PolicyMapper {

    @Mapping(target = "version", source = "currentVersion.versionNumber")
    @Mapping(target = "kind", source = "currentVersion.kind")
    @Mapping(target = "tiers", source = "currentVersion.tiers")
    PolicyResponse toResponse(Policy policy);

    PolicyTierResponse toResponse(PolicyTier tier);
}
