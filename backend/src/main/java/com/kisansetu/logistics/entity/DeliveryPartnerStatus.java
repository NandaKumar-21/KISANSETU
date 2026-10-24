package com.kisansetu.logistics.entity;

import com.kisansetu.order.OrderState.DeliveryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_partner_status")
@Getter
@Setter
public class DeliveryPartnerStatus {

    public enum PartnerAvailability {
        AVAILABLE, BUSY, OFFLINE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "partner_id", nullable = false, unique = true)
    private UUID partnerId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, length = 20)
    private PartnerAvailability status;

    @Column(name = "last_assigned_at")
    private Instant lastAssignedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        updatedAt = Instant.now();
        if (status == null) {
            status = PartnerAvailability.AVAILABLE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}