package com.kisansetu.logistics.repository;

import com.kisansetu.logistics.entity.DeliveryPartnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryPartnerStatusRepository extends JpaRepository<DeliveryPartnerStatus, UUID> {

    Optional<DeliveryPartnerStatus> findByPartnerId(UUID partnerId);

    List<DeliveryPartnerStatus> findByStatus(DeliveryPartnerStatus.PartnerAvailability status);
}