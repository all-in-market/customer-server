package com.example.allinmarket.domain.sellerdailystatistics.repository;

import com.example.allinmarket.domain.sellerdailystatistics.dto.DailyStatisticsResponse;

import java.time.LocalDate;

public interface CustomSellerDailyStatisticsRepository {
    DailyStatisticsResponse findRangedStatistics(Long sellerId, LocalDate from, LocalDate to);
}
