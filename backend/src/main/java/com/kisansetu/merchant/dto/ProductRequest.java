package com.kisansetu.merchant.dto;

import com.kisansetu.merchant.entity.Product;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name too long")
        String name,

        @Size(max = 2000, message = "Description too long")
        String description,

        @NotBlank(message = "Category is required")
        String category,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price cannot be negative")
        @DecimalMax(value = "9999999.99", message = "Price too high")
        BigDecimal price,

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity,

        @NotBlank(message = "Unit is required")
        @Size(max = 20, message = "Unit too long")
        String unit,

        String imageUrl,

        @Min(value = 1, message = "Stock threshold must be at least 1")
        Integer stockThreshold
) {

    public static ProductRequest from(Product p) {
        return new ProductRequest(
                p.getName(), p.getDescription(), p.getCategory(), p.getPrice(),
                p.getQuantity(), p.getUnit(), p.getImageUrl(), p.getStockThreshold());
    }
}