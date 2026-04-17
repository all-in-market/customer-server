package com.example.allinmarket.seller.dailystatistics.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.sellerdailystatistics.dto.response.DailyStatisticsResponse;
import com.example.allinmarket.domain.sellerdailystatistics.entity.SellerDailyStatistics;
import com.example.allinmarket.domain.sellerdailystatistics.repository.SellerDailyStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDailyStatisticsService {
    private final SellerDailyStatisticsRepository sellerDailyStatisticsRepository;

    public DailyStatisticsResponse getDailyStatistics(Long sellerId, LocalDate date) {
        SellerDailyStatistics statistics = sellerDailyStatisticsRepository.findBySellerIdAndStatDate(sellerId, date).orElseThrow(
                () -> new BaseException(ErrorEnum.STATISTICS_NOT_FOUND)
        );

        return DailyStatisticsResponse.from(statistics);
    }
}
