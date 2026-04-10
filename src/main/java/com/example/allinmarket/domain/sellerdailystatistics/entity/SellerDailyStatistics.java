package com.example.allinmarket.domain.sellerdailystatistics.entity;

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
public class SellerDailyStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "total_orders", nullable = false)
    private int totalOrders;

    @Column(name = "total_items", nullable = false)
    private int totalItems;

    @PositiveOrZero
    @Column(name = "total_sales", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "total_refunds", nullable = false)
    private int totalRefunds;

    @PositiveOrZero
    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @PositiveOrZero
    @Column(name = "net_sales", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSales;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static SellerDailyStatistics of(
            Seller seller,
            LocalDate statDate,
            Integer totalOrders,
            Integer totalItems,
            BigDecimal totalSales,
            Integer totalRefunds,
            BigDecimal refundAmount,
            BigDecimal netSales
    ) {
        SellerDailyStatistics statistics = new SellerDailyStatistics();
        statistics.seller = seller;
        statistics.statDate = statDate;
        statistics.totalOrders = totalOrders != null ? totalOrders : 0;
        statistics.totalItems = totalItems != null ? totalItems : 0;
        statistics.totalSales = totalSales != null ? totalSales : BigDecimal.ZERO;
        statistics.totalRefunds = totalRefunds != null ? totalRefunds : 0;
        statistics.refundAmount = refundAmount != null ? refundAmount : BigDecimal.ZERO;
        statistics.netSales = netSales != null ? netSales : BigDecimal.ZERO;
        statistics.createdAt = LocalDateTime.now();
        return statistics;
    }
}