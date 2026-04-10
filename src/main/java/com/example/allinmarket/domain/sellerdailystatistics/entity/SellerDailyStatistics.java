package com.example.allinmarket.domain.sellerdailystatistics.entity;

import com.example.allinmarket.common.entity.CreatableEntity;
import com.example.allinmarket.seller.entity.Seller;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "seller_daily_statistics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerDailyStatistics extends CreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "total_orders")
    private int totalOrders = 0;

    @Column(name = "total_items")
    private int totalItems = 0;

    @PositiveOrZero
    @Column(name = "total_sales", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(name = "total_refunds")
    private int totalRefunds = 0;

    @PositiveOrZero
    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @PositiveOrZero
    @Column(name = "net_sales", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSales = BigDecimal.ZERO;

    public static SellerDailyStatistics of(
            Seller seller,
            LocalDate statDate,
            int totalOrders,
            int totalItems,
            int totalRefunds,
            BigDecimal totalSales,
            BigDecimal refundAmount,
            BigDecimal netSales
    ) {
        SellerDailyStatistics statistics = new SellerDailyStatistics();
        statistics.seller = seller;
        statistics.statDate = statDate;
        statistics.totalOrders = totalOrders;
        statistics.totalItems = totalItems;
        statistics.totalRefunds = totalRefunds;
        statistics.totalSales = totalSales != null ? totalSales : BigDecimal.ZERO;
        statistics.refundAmount = refundAmount != null ? refundAmount : BigDecimal.ZERO;
        statistics.netSales = netSales != null ? netSales : BigDecimal.ZERO;
        return statistics;
    }
}