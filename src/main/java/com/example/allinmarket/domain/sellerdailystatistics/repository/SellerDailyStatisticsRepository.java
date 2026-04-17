package com.example.allinmarket.domain.sellerdailystatistics.repository;

import com.example.allinmarket.domain.sellerdailystatistics.entity.SellerDailyStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.time.LocalDate;

public interface SellerDailyStatisticsRepository extends JpaRepository<SellerDailyStatistics, Long>, CustomSellerDailyStatisticsRepository{
    boolean existsBySellerIdAndStatDate(Long sellerId, LocalDate statDate);
    Optional<SellerDailyStatistics> findBySellerIdAndStatDate(Long sellerId, LocalDate statDate);
}
