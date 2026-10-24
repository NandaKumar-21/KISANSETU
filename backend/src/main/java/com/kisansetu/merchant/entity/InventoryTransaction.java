package com.kisansetu.merchant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit trail of inventory changes per product.
 */
@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "change_qty", nullable = false)
    private Integer changeQty;

    @Column(nullable = false)
    private String reason;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}