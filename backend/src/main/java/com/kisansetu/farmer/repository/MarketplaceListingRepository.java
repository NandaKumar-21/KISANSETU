package com.kisansetu.farmer.repository;

import com.kisansetu.farmer.entity.MarketplaceListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, UUID> {

    List<MarketplaceListing> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId);

    @Query("""
            select l from MarketplaceListing l
            where l.status = 'ACTIVE'
              and (:search is null or lower(l.title) like lower(concat('%', cast(:search as string), '%'))
                   or lower(coalesce(l.category,'')) like lower(concat('%', cast(:search as string), '%')))
              and (:category is null or l.category = :category)
              and (:minPrice is null or l.price >= :minPrice)
              and (:maxPrice is null or l.price <= :maxPrice)
              and (:farmerId is null or l.farmerId = :farmerId)
            """)
    Page<MarketplaceListing> search(
            @Param("search") String search,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("farmerId") UUID farmerId,
            Pageable pageable);

    @Query("select distinct l.category from MarketplaceListing l where l.status = 'ACTIVE' and l.category is not null")
    List<String> findActiveCategories();

    long countByFarmerIdAndStatus(UUID farmerId, MarketplaceListing.ListingStatus status);

    List<MarketplaceListing> findByFarmerIdAndStatus(UUID farmerId, MarketplaceListing.ListingStatus status);
}