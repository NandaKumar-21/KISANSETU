package com.kisansetu.customer.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PriceCompareRow(UUID listingId, UUID farmerId, String farmerName, BigDecimal price,
                              BigDecimal quantity, String unit, String location, String farmingMethod,
                              double avgRating) {
}