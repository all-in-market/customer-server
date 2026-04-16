package com.example.allinmarket.domain.sellerdailystatistics.dto;

import com.example.allinmarket.domain.sellerdailystatistics.entity.SellerDailyStatistics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyStatisticsResponse(
        Long sellerId,
        LocalDate from,
        LocalDate to,
        int totalOrders,
        int totalItems,
        BigDecimal totalSales,
        int totalRefunds,
        BigDecimal refundAmount,
        BigDecimal netSales
) {
    public static DailyStatisticsResponse from(SellerDailyStatistics sellerDailyStatistics) {
        return new DailyStatisticsResponse(
                sellerDailyStatistics.getSeller().getId(),
                sellerDailyStatistics.getStatDate(),
                sellerDailyStatistics.getStatDate(),
                sellerDailyStatistics.getTotalOrders(),
                sellerDailyStatistics.getTotalItems(),
                sellerDailyStatistics.getTotalSales(),
                sellerDailyStatistics.getTotalRefunds(),
                sellerDailyStatistics.getRefundAmount(),
                sellerDailyStatistics.getNetSales()
        );
    }

    public static DailyStatisticsResponse of(
            Long sellerId,
            LocalDate from,
            LocalDate to,
            int totalOrders,
            int totalItems,
            BigDecimal totalSales,
            int totalRefunds,
            BigDecimal refundAmount,
            BigDecimal netSales
    ) {
        return new DailyStatisticsResponse(
                sellerId,
                from,
                to,
                totalOrders,
                totalItems,
                totalSales,
                totalRefunds,
                refundAmount,
                netSales
        );
    }
}