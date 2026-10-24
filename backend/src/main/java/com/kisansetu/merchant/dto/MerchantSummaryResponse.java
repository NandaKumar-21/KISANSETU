package com.kisansetu.merchant.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Aggregated merchant card used by the farmer merchant-marketplace.
 */
public record MerchantSummaryResponse(
        UUID merchantId,
        String fullName,
        String city,
        String state,
        String avatarUrl,
        Double latitude,
        Double longitude,
        Double distanceKm,
        long itemCount,
        BigDecimal avgPrice,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<String> categories
) {
}