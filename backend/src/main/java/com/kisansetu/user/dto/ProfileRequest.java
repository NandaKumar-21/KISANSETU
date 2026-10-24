package com.kisansetu.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
        String fullName,

        @Size(max = 20, message = "Phone number is too long")
        String phone,

        String address,
        String city,
        String state,

        @Size(max = 10, message = "Pincode must be at most 10 characters")
        String pincode,

        String avatarUrl,

        BigDecimal latitude,
        BigDecimal longitude
) {
}