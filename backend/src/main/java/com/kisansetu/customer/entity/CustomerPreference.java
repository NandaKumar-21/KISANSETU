package com.kisansetu.customer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_preferences")
@Getter
@Setter
public class CustomerPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private UUID customerId;

    @Column(name = "preferred_categories", columnDefinition = "text[]")
    private String[] preferredCategories;

    @Column(name = "preferred_farmers", columnDefinition = "uuid[]")
    private UUID[] preferredFarmers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "last_recommendations", columnDefinition = "jsonb")
    private String lastRecommendations;

    @Column(name = "recommendations_updated_at")
    private Instant recommendationsUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}