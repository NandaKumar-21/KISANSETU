package com.kisansetu.customer.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PriceCompareGroup(String displayName, String key, List<PriceCompareRow> rows) {
}