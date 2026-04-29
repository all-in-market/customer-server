package com.example.allinmarket.domain.sellerdailystatistics.repository;

import com.example.allinmarket.domain.sellerdailystatistics.entity.SellerDailyStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface SellerDailyStatisticsRepository extends JpaRepository<SellerDailyStatistics, Long>, CustomSellerDailyStatisticsRepository{
    Optional<SellerDailyStatistics> findBySellerIdAndStatDate(Long sellerId, LocalDate statDate);

    @Query("SELECT s.seller.id FROM SellerDailyStatistics s WHERE s.statDate = :date")
    List<Long> findExistingSellerIds(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(s.netSales), 0) FROM SellerDailyStatistics s WHERE s.seller.id = :sellerId AND s.statDate BETWEEN :start AND :end")
    BigDecimal sumNetSalesBySellerAndPeriod(@Param("sellerId") Long sellerId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
