package com.kisansetu.customer.repository;

import com.kisansetu.customer.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<WishlistItem> findByCustomerIdAndListingId(UUID customerId, UUID listingId);

    Optional<WishlistItem> findByCustomerIdAndFarmerId(UUID customerId, UUID farmerId);

    long countByCustomerId(UUID customerId);
}