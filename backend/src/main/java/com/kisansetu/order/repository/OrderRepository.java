package com.kisansetu.order.repository;

import com.kisansetu.order.entity.Order;
import com.kisansetu.order.OrderState.MerchantOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId);

    List<Order> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Page<Order> findByMerchantId(UUID merchantId, Pageable pageable);

    List<Order> findByMerchantIdAndStatus(UUID merchantId, MerchantOrderStatus status);

    long countByMerchantIdAndStatus(UUID merchantId, MerchantOrderStatus status);
}