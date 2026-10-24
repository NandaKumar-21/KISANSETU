package com.kisansetu.merchant.repository;

import com.kisansetu.merchant.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(UUID productId);

    List<InventoryTransaction> findByProductIdInOrderByCreatedAtDesc(List<UUID> productIds);
}