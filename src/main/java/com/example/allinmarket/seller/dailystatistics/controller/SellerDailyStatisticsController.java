package com.example.allinmarket.seller.dailystatistics.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.seller.dailystatistics.dto.DailyStatisticsResponse;

import com.example.allinmarket.seller.dailystatistics.service.SellerDailyStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/statistics")
public class SellerDailyStatisticsController {

    private final SellerDailyStatisticsService sellerDailyStatisticsService;

    // 특정일 조회
    ///  TODO : 특정일 조회
    @GetMapping("/daily/{date}")
    public ResponseEntity<ApiResponse<DailyStatisticsResponse>> getDailyStatistics(
            @PathVariable LocalDate date
            ) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.READ_SUCCESS, sellerDailyStatisticsService.getDailyStatistics(sellerId, date)));
    }


    // 특정 기간 조회

    ///  TODO : 특정 기간 조회
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DailyStatisticsResponse>> getRangedStatistics(
            @RequestParam String from,
            @RequestParam String to
    ) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        sellerDailyStatisticsService.getRangedStatistics(sellerId, from, to)
                )
        );
    }
}
