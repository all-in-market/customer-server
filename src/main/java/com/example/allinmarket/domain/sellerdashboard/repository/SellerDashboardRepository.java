package com.example.allinmarket.domain.sellerdashboard.repository;

import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface SellerDashboardRepository extends JpaRepository<SellerDashboard, Long> {

    Optional<SellerDashboard> findBySellerId(Long sellerId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE SellerDashboard d SET
              d.totalOrders        = d.totalOrders + 1,
              d.totalProductsSold  = d.totalProductsSold + :productsSold,
              d.totalSales         = d.totalSales + :salesAmount,
              d.feeAmount          = (d.totalSales + :salesAmount - d.refundAmount ) * :commissionRate,
              d.settlementAmount   = (d.totalSales + :salesAmount - d.refundAmount) * (1 - :commissionRate)
            WHERE d.seller.id = :sellerId
            """)
    void addOrder(@Param("sellerId") Long sellerId,
                  @Param("salesAmount") BigDecimal salesAmount,
                  @Param("productsSold") int productsSold,
                  @Param("commissionRate") BigDecimal commissionRate);
}
