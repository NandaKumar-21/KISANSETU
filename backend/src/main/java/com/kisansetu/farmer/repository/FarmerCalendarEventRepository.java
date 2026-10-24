package com.kisansetu.farmer.repository;

import com.kisansetu.farmer.entity.FarmerCalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FarmerCalendarEventRepository extends JpaRepository<FarmerCalendarEvent, UUID> {

    List<FarmerCalendarEvent> findByFarmerIdOrderByEventDateAsc(UUID farmerId);

    List<FarmerCalendarEvent> findByFarmerIdAndEventDateBetweenOrderByEventDateAsc(
            UUID farmerId, LocalDate from, LocalDate to);

    List<FarmerCalendarEvent> findByFarmerIdAndCompletedFalseAndEventDateGreaterThanEqualOrderByEventDateAsc(
            UUID farmerId, LocalDate date);

    void deleteByFarmerId(UUID farmerId);
}