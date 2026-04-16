package com.example.allinmarket.domain.sellerdailystatistics.repository;

import com.example.allinmarket.domain.sellerdailystatistics.entity.SellerDailyStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface SellerDailyStatisticsRepository extends JpaRepository<SellerDailyStatistics, Long> {
    boolean existsBySellerIdAndStatDate(Long sellerId, LocalDate statDate);
}
