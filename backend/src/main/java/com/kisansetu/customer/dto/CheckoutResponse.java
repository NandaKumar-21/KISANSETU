package com.kisansetu.customer.dto;

import java.math.BigDecimal;

public record CheckoutResponse(int ordersCreated, BigDecimal grandTotal, String message) {
}