package com.example.allinmarket.seller.dailyStatistics.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.sellerdailystatistics.dto.DailyStatisticsResponse;
import com.example.allinmarket.domain.sellerdailystatistics.repository.SellerDailyStatisticsRepository;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDailyStatisticsService {

    private final SellerDailyStatisticsRepository sellerDailyStatisticsRepository;
    private final SellerRepository sellerRepository;

    public DailyStatisticsResponse getRangedStatistics(Long sellerId, String from, String to) {

        if(!sellerRepository.existsByIdAndDeletedAtIsNull(sellerId)) {
            throw new BaseException(ErrorEnum.SELLER_NOT_FOUND);
        }

        LocalDate startTime = from != null ?
                LocalDate.parse(from) : null;
        LocalDate endTime = to != null ?
                LocalDate.parse(to) : null;

        return sellerDailyStatisticsRepository.findRangedStatistics(sellerId, startTime, endTime);


    }
}
