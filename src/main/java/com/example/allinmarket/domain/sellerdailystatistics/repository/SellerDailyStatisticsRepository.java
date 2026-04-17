package com.example.allinmarket.domain.sellerdailystatistics.repository;

import com.example.allinmarket.domain.sellerdailystatistics.entity.SellerDailyStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SellerDailyStatisticsRepository extends JpaRepository<SellerDailyStatistics, Long> {
    Optional<SellerDailyStatistics> findBySellerIdAndStatDate(Long sellerId, LocalDate statDate);
}
