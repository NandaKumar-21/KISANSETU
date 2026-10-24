package com.kisansetu.farmer.repository;

import com.kisansetu.farmer.entity.MarketplaceOrder;
import com.kisansetu.order.OrderState.MarketplaceOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrder, UUID> {

    List<MarketplaceOrder> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);

    List<MarketplaceOrder> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId);

    long countByFarmerIdAndStatus(UUID farmerId, MarketplaceOrderStatus status);
}