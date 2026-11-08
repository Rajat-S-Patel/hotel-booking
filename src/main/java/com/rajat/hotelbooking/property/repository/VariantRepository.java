package com.rajat.hotelbooking.property.repository;

import com.rajat.hotelbooking.property.entity.Variant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VariantRepository extends Repository<Variant, UUID> {

    Optional<Variant> findById(UUID id);

    @Query("""
            select v from Variant v
            join fetch v.roomType rt
            join fetch rt.property
            where v.id = :id
            """)
    Optional<Variant> findWithRoomTypeAndPropertyById(@Param("id") UUID id);
}
