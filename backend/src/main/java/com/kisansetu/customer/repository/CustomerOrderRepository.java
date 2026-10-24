package com.kisansetu.customer.repository;

import com.kisansetu.customer.entity.CustomerOrder;
import com.kisansetu.order.OrderState.CustomerOrderStatus;
import com.kisansetu.order.OrderState.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<CustomerOrder> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId);

    List<CustomerOrder> findByDeliveryPartnerIdOrderByCreatedAtDesc(UUID partnerId);

    @Query("select o from CustomerOrder o where o.deliveryPartnerId = :partnerId " +
            "and (o.deliveryStatus is null or o.deliveryStatus not in :excluded) order by o.createdAt desc")
    List<CustomerOrder> findPartnerDeliveries(@Param("partnerId") UUID partnerId,
                                              @Param("excluded") List<DeliveryStatus> excluded);

    long countByCustomerIdAndStatusIn(UUID customerId, List<CustomerOrderStatus> statuses);

    long countByFarmerIdAndStatusIn(UUID farmerId, List<CustomerOrderStatus> statuses);

    long countByFarmerIdAndStatus(UUID farmerId, CustomerOrderStatus status);

    long countByDeliveryPartnerIdAndStatus(UUID partnerId, CustomerOrderStatus status);

    long countByDeliveryPartnerIdAndDeliveryStatusIn(UUID partnerId, List<DeliveryStatus> statuses);

    @Query("select count(o) from CustomerOrder o where o.deliveryPartnerId = :partnerId and o.deliveredAt >= :start")
    long countDeliveredSince(@Param("partnerId") UUID partnerId, @Param("start") Instant start);

    @Query("select coalesce(sum(o.totalPrice), 0) from CustomerOrder o " +
            "where o.deliveryPartnerId = :partnerId and o.deliveredAt >= :start")
    java.math.BigDecimal sumDeliveredValueSince(@Param("partnerId") UUID partnerId, @Param("start") Instant start);

    @Query("select o from CustomerOrder o where o.deliveryPartnerId = :partnerId " +
            "and (o.deliveryStatus is null or o.deliveryStatus not in :excluded) order by o.createdAt asc")
    List<CustomerOrder> findActiveDeliveriesAsc(@Param("partnerId") UUID partnerId,
                                                @Param("excluded") List<DeliveryStatus> excluded);

    @Query("select count(o) from CustomerOrder o where o.deliveryPartnerId = :partnerId " +
            "and (o.deliveryStatus is null or o.deliveryStatus not in :excluded)")
    long countActiveDeliveries(@Param("partnerId") UUID partnerId, @Param("excluded") List<DeliveryStatus> excluded);
}