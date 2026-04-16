package com.example.allinmarket.domain.sellerdailystatistics.dto;

import com.example.allinmarket.domain.sellerdailystatistics.entity.SellerDailyStatistics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyStatisticsResponse(
        LocalDate statDate,
        int totalOrders,
        int totalItems,
        BigDecimal totalSales,
        int totalRefunds,
        BigDecimal refundAmount,
        BigDecimal netSales
) {
    public static DailyStatisticsResponse from(SellerDailyStatistics sellerDailyStatistics) {
        return new DailyStatisticsResponse(
                sellerDailyStatistics.getStatDate(),
                sellerDailyStatistics.getTotalOrders(),
                sellerDailyStatistics.getTotalItems(),
                sellerDailyStatistics.getTotalSales(),
                sellerDailyStatistics.getTotalRefunds(),
                sellerDailyStatistics.getRefundAmount(),
                sellerDailyStatistics.getNetSales()
        );
    }
}