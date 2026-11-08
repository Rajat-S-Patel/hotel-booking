package com.rajat.hotelbooking.booking.repository;

import com.rajat.hotelbooking.booking.entity.Customer;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends Repository<Customer, UUID> {

    Customer save(Customer customer);

    Optional<Customer> findByEmail(String email);
}
