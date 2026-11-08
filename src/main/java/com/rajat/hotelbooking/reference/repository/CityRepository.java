package com.rajat.hotelbooking.reference.repository;

import com.rajat.hotelbooking.reference.entity.City;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CityRepository extends Repository<City, UUID> {

    City save(City city);

    Optional<City> findById(UUID id);

    Optional<City> findByNameIgnoreCase(String name);

    List<City> findAllByOrderByNameAsc();
}
