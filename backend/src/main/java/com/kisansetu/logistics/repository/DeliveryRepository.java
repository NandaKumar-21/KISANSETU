package com.kisansetu.logistics.repository;

import com.kisansetu.logistics.entity.Delivery;
import com.kisansetu.logistics.entity.DeliveryPartnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByOrderId(UUID orderId);

    List<Delivery> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId);
}