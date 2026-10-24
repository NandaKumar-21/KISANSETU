package com.kisansetu.customer.repository;

import com.kisansetu.customer.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    List<CustomerAddress> findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(UUID customerId);

    List<CustomerAddress> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    long countByCustomerId(UUID customerId);
}