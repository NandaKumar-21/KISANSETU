package com.kisansetu.customer.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FarmerSummaryResponse(
        UUID farmerId,
        String fullName,
        String city,
        String state,
        String avatarUrl,
        Double distanceKm,
        long cropCount,
        BigDecimal avgPrice,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        double avgRating,
        long totalReviews,
        List<String> categories
) {
}