package com.kisansetu.order.dto;

import com.kisansetu.merchant.entity.Product;
import com.kisansetu.order.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID farmerId,
        String farmerName,
        UUID merchantId,
        String merchantName,
        UUID productId,
        String productName,
        String productUnit,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {

    public static OrderResponse from(Order order, Product product, String farmerName, String merchantName) {
        return new OrderResponse(
                order.getId(),
                order.getFarmerId(),
                farmerName,
                order.getMerchantId(),
                merchantName,
                order.getProductId(),
                product != null ? product.getName() : null,
                product != null ? product.getUnit() : null,
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalPrice(),
                order.getStatus().name().toLowerCase(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}