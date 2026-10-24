package com.kisansetu.ai.repository;

import com.kisansetu.ai.entity.CropPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CropPlanRepository extends JpaRepository<CropPlan, UUID> {

    List<CropPlan> findTop10ByFarmerIdOrderByCreatedAtDesc(UUID farmerId);
}