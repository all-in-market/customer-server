package com.example.allinmarket.domain.sellerdailystatistics.repository;

import com.example.allinmarket.domain.sellerdailystatistics.entity.SellerDailyStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface SellerDailyStatisticsRepository extends JpaRepository<SellerDailyStatistics, Long>, CustomSellerDailyStatisticsRepository{
    Optional<SellerDailyStatistics> findBySellerIdAndStatDate(Long sellerId, LocalDate statDate);

    @Query("SELECT s.seller.id FROM SellerDailyStatistics s WHERE s.statDate = :date")
    List<Long> findExistingSellerIds(@Param("date") LocalDate date);
}
