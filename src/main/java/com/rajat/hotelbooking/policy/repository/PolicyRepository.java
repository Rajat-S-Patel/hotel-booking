package com.rajat.hotelbooking.policy.repository;

import com.rajat.hotelbooking.policy.entity.Policy;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyRepository extends Repository<Policy, UUID> {

    Policy save(Policy policy);

    Optional<Policy> findById(UUID id);

    List<Policy> findByOwnerIdIsNullOrderByNameAsc();
}
