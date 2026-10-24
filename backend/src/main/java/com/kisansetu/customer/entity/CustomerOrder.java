package com.kisansetu.customer.entity;

import com.kisansetu.order.OrderState.CustomerOrderStatus;
import com.kisansetu.order.OrderState.DeliveryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Customer -> Farmer order (fresh produce).
 */
@Entity
@Table(name = "customer_orders")
@Getter
@Setter
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "farmer_id", nullable = false)
    private UUID farmerId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, length = 20)
    private CustomerOrderStatus status;

    @Column(name = "delivery_address_id")
    private UUID deliveryAddressId;

    @Column(name = "delivery_preference")
    private String deliveryPreference;

    @Column(name = "estimated_delivery")
    private Instant estimatedDelivery;

    @Column(length = 2000)
    private String notes;

    @Column(name = "farmer_notes", length = 2000)
    private String farmerNotes;

    @Column(name = "farmer_contact_visible")
    private boolean farmerContactVisible;

    @Column(name = "contact_disabled_at")
    private Instant contactDisabledAt;

    @Column(name = "packed_at")
    private Instant packedAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "delivery_partner_id")
    private UUID deliveryPartnerId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "delivery_status", length = 30)
    private DeliveryStatus deliveryStatus;

    @Column(name = "pickup_time")
    private Instant pickupTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = CustomerOrderStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}