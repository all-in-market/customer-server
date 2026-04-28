package com.example.allinmarket.seller.dailystatistics.dto;

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
    public DailyStatisticsResponse(Long sellerId, Integer totalOrders, Integer totalItems,
                                   BigDecimal totalSales, Integer totalRefunds,
                                   BigDecimal refundAmount, BigDecimal netSales) {
        this(sellerId, null, null, totalOrders, totalItems, totalSales, totalRefunds, refundAmount, netSales);
    }

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
}
