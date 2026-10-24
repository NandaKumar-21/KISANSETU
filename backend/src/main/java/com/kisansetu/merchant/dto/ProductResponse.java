package com.kisansetu.merchant.dto;

import com.kisansetu.merchant.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID merchantId,
        String merchantName,
        String name,
        String description,
        String category,
        BigDecimal price,
        Integer quantity,
        String unit,
        String imageUrl,
        Integer stockThreshold,
        boolean lowStock,
        boolean inStock,
        Instant createdAt
) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getMerchantId(), null, p.getName(), p.getDescription(),
                p.getCategory(), p.getPrice(), p.getQuantity(), p.getUnit(), p.getImageUrl(),
                p.getStockThreshold(), p.isLowStock(), p.getQuantity() != null && p.getQuantity() > 0,
                p.getCreatedAt());
    }

    public static ProductResponse withMerchantName(Product p, String merchantName) {
        ProductResponse r = from(p);
        return new ProductResponse(r.id(), r.merchantId(), merchantName, r.name(), r.description(),
                r.category(), r.price(), r.quantity(), r.unit(), r.imageUrl(), r.stockThreshold(),
                r.lowStock(), r.inStock(), r.createdAt());
    }
}