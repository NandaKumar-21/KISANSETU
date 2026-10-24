package com.kisansetu.customer.repository;

import com.kisansetu.customer.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCustomerIdOrderByCreatedAtAsc(UUID customerId);

    Optional<CartItem> findByCustomerIdAndListingId(UUID customerId, UUID listingId);

    long countByCustomerId(UUID customerId);

    void deleteByCustomerId(UUID customerId);
}