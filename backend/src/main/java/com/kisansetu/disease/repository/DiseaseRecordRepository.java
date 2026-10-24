package com.kisansetu.disease.repository;

import com.kisansetu.disease.entity.DiseaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiseaseRecordRepository extends JpaRepository<DiseaseRecord, UUID> {

    List<DiseaseRecord> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId);

    long countByFarmerId(UUID farmerId);
}