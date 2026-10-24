package com.kisansetu.customer.repository;

import com.kisansetu.customer.entity.CustomerPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerPreferenceRepository extends JpaRepository<CustomerPreference, UUID> {

    Optional<CustomerPreference> findByCustomerId(UUID customerId);
}