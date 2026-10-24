package com.kisansetu.customer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "availability_alerts")
@Getter
@Setter
public class AvailabilityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "listing_id")
    private UUID listingId;

    private String category;

    @Column(name = "farmer_id")
    private UUID farmerId;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        isActive = true;
    }
}