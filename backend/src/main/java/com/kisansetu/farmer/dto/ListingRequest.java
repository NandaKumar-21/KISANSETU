package com.kisansetu.farmer.dto;

import com.kisansetu.farmer.entity.MarketplaceListing;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ListingRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title too long")
        String title,

        @Size(max = 2000, message = "Description too long")
        String description,

        @NotBlank(message = "Category is required")
        String category,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be positive")
        BigDecimal price,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", message = "Quantity cannot be negative")
        BigDecimal quantity,

        @NotBlank(message = "Unit is required")
        @Size(max = 20, message = "Unit too long")
        String unit,

        String imageUrl,
        String location,
        String variety,
        String farmingMethod,
        LocalDate harvestDate,
        BigDecimal latitude,
        BigDecimal longitude
) {
}