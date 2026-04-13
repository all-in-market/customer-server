package com.example.allinmarket.domain.sellerdashboard.entity;

import com.example.allinmarket.common.entity.ModifiableEntity;
import com.example.allinmarket.seller.entity.Seller;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.example.allinmarket.seller.consts.sellerConsts.COMMISSION_RATE;

@Getter
@Entity
@Table(name = "seller_dashboard")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerDashboard extends ModifiableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @PositiveOrZero
    @Column(name = "total_orders")
    private int totalOrders;

    @PositiveOrZero
    @Column(name = "total_sales", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSales;

    @PositiveOrZero
    @Column(name = "total_products_sold")
    private int totalProductsSold;

    @PositiveOrZero
    @Column(name = "total_refunds")
    private int totalRefunds;

    @PositiveOrZero
    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @PositiveOrZero
    @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "settlement_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal settlementAmount;

    public static SellerDashboard of(
            Seller seller,
            LocalDate statDate,
            int totalOrders,
            int totalProductsSold,
            int totalRefunds,
            BigDecimal totalSales,
            BigDecimal refundAmount
    ) {
        SellerDashboard dashboard = new SellerDashboard();

        dashboard.seller = seller;
        dashboard.statDate = statDate == null ? LocalDate.now() : statDate;
        dashboard.totalOrders = totalOrders;
        dashboard.totalProductsSold = totalProductsSold;
        dashboard.totalRefunds = totalRefunds;
        dashboard.totalSales = totalSales != null ? totalSales : BigDecimal.ZERO;
        dashboard.refundAmount = refundAmount != null ? refundAmount : BigDecimal.ZERO;
        dashboard.feeAmount = dashboard.totalSales.multiply(COMMISSION_RATE);
        dashboard.settlementAmount = dashboard.totalSales.subtract(dashboard.refundAmount.add(dashboard.feeAmount));
        return dashboard;
    }
}
