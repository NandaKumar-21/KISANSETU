package com.kisansetu.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit trail of order status transitions.
 */
@Entity
@Table(name = "order_status_history")
@Getter
@Setter
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_type", nullable = false)
    private String orderType;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(name = "changed_by")
    private UUID changedBy;

    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}