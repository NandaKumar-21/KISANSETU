package com.kisansetu.customer.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckoutRequest(
        @NotNull(message = "Delivery address is required")
        UUID deliveryAddressId,

        String deliveryPreference,
        String notes
) {
}