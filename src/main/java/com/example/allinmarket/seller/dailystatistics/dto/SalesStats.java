package com.example.allinmarket.seller.dailystatistics.dto;

import java.math.BigDecimal;

public record SalesStats(
        Long totalOrders,
        Long totalItems,
        BigDecimal totalSales
) {
}
