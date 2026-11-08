package com.rajat.hotelbooking.property.repository;

import com.rajat.hotelbooking.property.entity.Owner;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface OwnerRepository extends Repository<Owner, UUID> {

    Owner save(Owner owner);

    Optional<Owner> findById(UUID id);

    boolean existsByContactEmail(String contactEmail);
}
