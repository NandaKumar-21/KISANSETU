package com.kisansetu.customer.dto;

import com.kisansetu.customer.entity.CustomerOrder;
import com.kisansetu.farmer.entity.MarketplaceListing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerOrderResponse(
        UUID id,
        UUID customerId,
        String customerName,
        UUID farmerId,
        String farmerName,
        UUID listingId,
        String listingTitle,
        String listingUnit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String status,
        String deliveryPreference,
        Instant estimatedDelivery,
        String notes,
        String farmerNotes,
        boolean farmerContactVisible,
        Instant packedAt,
        Instant dispatchedAt,
        Instant deliveredAt,
        UUID deliveryPartnerId,
        String deliveryStatus,
        Instant pickupTime,
        Instant createdAt,
        Instant updatedAt
) {

    public static CustomerOrderResponse from(CustomerOrder o, MarketplaceListing listing,
                                             String customerName, String farmerName) {
        return new CustomerOrderResponse(
                o.getId(), o.getCustomerId(), customerName, o.getFarmerId(), farmerName,
                o.getListingId(),
                listing != null ? listing.getTitle() : null,
                listing != null ? listing.getUnit() : null,
                o.getQuantity(), o.getUnitPrice(), o.getTotalPrice(),
                o.getStatus().name().toLowerCase(),
                o.getDeliveryPreference(), o.getEstimatedDelivery(),
                o.getNotes(), o.getFarmerNotes(), o.isFarmerContactVisible(),
                o.getPackedAt(), o.getDispatchedAt(), o.getDeliveredAt(),
                o.getDeliveryPartnerId(),
                o.getDeliveryStatus() != null ? o.getDeliveryStatus().name().toLowerCase() : null,
                o.getPickupTime(), o.getCreatedAt(), o.getUpdatedAt());
    }
}