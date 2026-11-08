package com.rajat.hotelbooking.reference.repository;

import com.rajat.hotelbooking.reference.entity.City;
import com.rajat.hotelbooking.reference.entity.Locality;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocalityRepository extends Repository<Locality, UUID> {

    Locality save(Locality locality);

    Optional<Locality> findById(UUID id);

    Optional<Locality> findByCityAndNameIgnoreCase(City city, String name);

    List<Locality> findByCityOrderByNameAsc(City city);
}
