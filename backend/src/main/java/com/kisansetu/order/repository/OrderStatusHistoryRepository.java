package com.kisansetu.order.repository;

import com.kisansetu.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    List<OrderStatusHistory> findByOrderTypeAndOrderIdOrderByCreatedAtAsc(String orderType, UUID orderId);
}