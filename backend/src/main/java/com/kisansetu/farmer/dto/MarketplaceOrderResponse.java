package com.kisansetu.farmer.dto;

import com.kisansetu.farmer.entity.MarketplaceOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MarketplaceOrderResponse(
        UUID id,
        UUID listingId,
        String listingTitle,
        String listingUnit,
        UUID buyerId,
        String buyerName,
        UUID farmerId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String status,
        String notes,
        Instant createdAt
) {

    public static MarketplaceOrderResponse from(MarketplaceOrder o, String listingTitle,
                                                String listingUnit, String buyerName) {
        return new MarketplaceOrderResponse(
                o.getId(), o.getListingId(), listingTitle, listingUnit, o.getBuyerId(),
                buyerName, o.getFarmerId(), o.getQuantity(), o.getUnitPrice(),
                o.getTotalPrice(), o.getStatus().name().toLowerCase(), o.getNotes(), o.getCreatedAt());
    }
}