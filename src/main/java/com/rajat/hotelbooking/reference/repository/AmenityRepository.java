package com.rajat.hotelbooking.reference.repository;

import com.rajat.hotelbooking.reference.entity.Amenity;
import com.rajat.hotelbooking.reference.entity.enums.AmenityScope;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AmenityRepository extends Repository<Amenity, UUID> {

    Amenity save(Amenity amenity);

    List<Amenity> findByCodeIn(Collection<String> codes);

    List<Amenity> findByScopeOrderByCodeAsc(AmenityScope scope);
}
