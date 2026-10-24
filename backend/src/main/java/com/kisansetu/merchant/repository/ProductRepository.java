package com.kisansetu.merchant.repository;

import com.kisansetu.merchant.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Page<Product> findByMerchantId(UUID merchantId, Pageable pageable);

    @Query("select p from Product p where p.quantity > 0 order by p.createdAt desc")
    List<Product> findInStockOrderByCreatedAtDesc();

    @Query("select p from Product p where p.quantity > 0")
    List<Product> findAllInStock();

    @Query("""
            select p from Product p
            where p.quantity > 0
              and (:search is null or lower(p.name) like lower(concat('%', cast(:search as string), '%'))
                   or lower(coalesce(p.category,'')) like lower(concat('%', cast(:search as string), '%')))
              and (:category is null or p.category = :category)
            order by p.createdAt desc
            """)
    List<Product> searchInStock(@Param("search") String search, @Param("category") String category);

    @Query("select distinct p.category from Product p where p.category is not null and p.category <> ''")
    List<String> findDistinctCategories();

    @Query("select count(p) from Product p where p.merchantId = :merchantId and p.quantity <= p.stockThreshold")
    long countLowStock(@Param("merchantId") UUID merchantId);

    @Query("""
            select p from Product p
            where p.quantity > 0
              and (:search is null or lower(p.name) like lower(concat('%', cast(:search as string), '%'))
                   or lower(coalesce(p.description,'')) like lower(concat('%', cast(:search as string), '%'))
                   or lower(coalesce(p.category,'')) like lower(concat('%', cast(:search as string), '%')))
              and (:category is null or p.category = :category)
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
            order by p.createdAt desc
            """)
    Page<Product> searchInStockPaginated(@Param("search") String search,
                                         @Param("category") String category,
                                         @Param("minPrice") java.math.BigDecimal minPrice,
                                         @Param("maxPrice") java.math.BigDecimal maxPrice,
                                         Pageable pageable);
}