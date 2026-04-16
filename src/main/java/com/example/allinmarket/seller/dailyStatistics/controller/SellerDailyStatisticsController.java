package com.example.allinmarket.seller.dailyStatistics.controller;

import com.example.allinmarket.domain.sellerdailystatistics.repository.SellerDailyStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/statistics")
public class SellerDailyStatisticsController {

    private final SellerDailyStatisticsRepository sellerDailyStatisticsRepository;

    // 특정일 조회
    ///  TODO : 특정일 조회


    // 특정 기간 조회
    ///  TODO : 특정 기간 조회
}
