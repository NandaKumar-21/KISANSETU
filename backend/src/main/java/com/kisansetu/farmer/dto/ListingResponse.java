package com.kisansetu.farmer.dto;

import com.kisansetu.farmer.entity.MarketplaceListing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        UUID farmerId,
        String farmerName,
        String title,
        String description,
        String category,
        BigDecimal price,
        BigDecimal quantity,
        String unit,
        String imageUrl,
        String location,
        String status,
        String variety,
        String farmingMethod,
        LocalDate harvestDate,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean available,
        Instant createdAt
) {

    public static ListingResponse from(MarketplaceListing l, String farmerName) {
        return new ListingResponse(
                l.getId(), l.getFarmerId(), farmerName, l.getTitle(), l.getDescription(),
                l.getCategory(), l.getPrice(), l.getQuantity(), l.getUnit(), l.getImageUrl(),
                l.getLocation(), l.getStatus().name().toLowerCase(), l.getVariety(),
                l.getFarmingMethod(), l.getHarvestDate(), l.getLatitude(), l.getLongitude(),
                l.isAvailable(), l.getCreatedAt());
    }
}