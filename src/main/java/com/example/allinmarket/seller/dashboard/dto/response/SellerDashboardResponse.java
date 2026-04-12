package com.example.allinmarket.seller.dashboard.dto.response;

import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SellerDashboardResponse(

        // 조회한 판매자 정보
        Long sellerId,

        // 조회 데이터 정보
        LocalDate statDate,

        // 매출 정보
        int totalOrders, // 오늘 주문 건수
        BigDecimal totalSales, // 오늘 매출
        int totalProductsSold, // 오늘 판매된 제품 수

        // 환불 정보
        int totalRefunds,
        BigDecimal refundAmount,

        // 정산 정보
        BigDecimal settlementAmount,
        BigDecimal feeAmount
) {
    public static SellerDashboardResponse from(SellerDashboard sellerDashboard) {
        return new SellerDashboardResponse(
                sellerDashboard.getSeller().getId(),
                sellerDashboard.getStatDate(),
                sellerDashboard.getTotalOrders(),
                sellerDashboard.getTotalSales(),
                sellerDashboard.getTotalProductsSold(),
                sellerDashboard.getTotalRefunds(),
                sellerDashboard.getRefundAmount(),
                sellerDashboard.getSettlementAmount(),
                sellerDashboard.getFeeAmount()
        );
    }
}
