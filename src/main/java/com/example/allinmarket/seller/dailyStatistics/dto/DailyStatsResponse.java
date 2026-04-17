package com.example.allinmarket.seller.dailyStatistics.dto;

import java.math.BigDecimal;

public record DailyStatsResponse(
        Long totalOrders,
        Long totalItems,
        Long totalRefunds,
        BigDecimal totalSales,
        BigDecimal refundAmount
) {
}
