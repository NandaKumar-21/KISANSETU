package com.kisansetu.customer.repository;

import com.kisansetu.customer.entity.FarmerRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FarmerRatingRepository extends JpaRepository<FarmerRating, UUID> {

    List<FarmerRating> findByFarmerId(UUID farmerId);

    List<FarmerRating> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId);

    Optional<FarmerRating> findByOrderId(UUID orderId);

    boolean existsByCustomerIdAndOrderId(UUID customerId, UUID orderId);

    @Query("select coalesce(avg(r.rating), 0) from FarmerRating r where r.farmerId = :farmerId")
    Double avgRating(@Param("farmerId") UUID farmerId);

    @Query("select count(r) from FarmerRating r where r.farmerId in :farmerIds")
    long countByFarmerIds(@Param("farmerIds") List<UUID> farmerIds);

    @Query("select r.farmerId, count(r), coalesce(avg(r.rating), 0) from FarmerRating r where r.farmerId in :farmerIds group by r.farmerId")
    List<Object[]> aggregateByFarmerIds(@Param("farmerIds") List<UUID> farmerIds);
}