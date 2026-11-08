package com.rajat.hotelbooking.policy.repository;

import com.rajat.hotelbooking.policy.entity.PolicyVersion;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface PolicyVersionRepository extends Repository<PolicyVersion, UUID> {

    Optional<PolicyVersion> findById(UUID id);
}
