package com.kisansetu.farmer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Farmer produce listing shown on the customer marketplace.
 */
@Entity
@Table(name = "marketplace_listings")
@Getter
@Setter
public class MarketplaceListing {

    public enum ListingStatus {
        ACTIVE, SOLD, EXPIRED, PAUSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "farmer_id", nullable = false)
    private UUID farmerId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false)
    private String unit;

    @Column(name = "image_url")
    private String imageUrl;

    private String location;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, length = 20)
    private ListingStatus status;

    private String variety;

    @Column(name = "farming_method")
    private String farmingMethod;

    @Column(name = "harvest_date")
    private LocalDate harvestDate;

    private BigDecimal latitude;
    private BigDecimal longitude;

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
            status = ListingStatus.ACTIVE;
        }
        if (farmingMethod == null) {
            farmingMethod = "conventional";
        }
        if (unit == null) {
            unit = "kg";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isAvailable() {
        return status == ListingStatus.ACTIVE && quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0;
    }
}