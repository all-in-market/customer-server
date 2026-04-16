package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.sellerdailystatistics.entity.SellerDailyStatistics;
import com.example.allinmarket.domain.sellerdailystatistics.repository.SellerDailyStatisticsRepository;
import com.example.allinmarket.seller.dailyStatistics.dto.DailyStatsResponse;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DailyStatisticsScheduler {

    private final SellerRepository sellerRepository;
    private final SellerDailyStatisticsRepository sellerDailyStatisticsRepository;
    private final OrderItemRepository orderItemRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void generateDailyStatistics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = yesterday.plusDays(1).atStartOfDay();

        // 전일 판매가 있었던 판매자만 조회
        List<Long> activeSellerIds = orderItemRepository.findActiveSellerIds(start, end);

        List<SellerDailyStatistics> statisticsList = new ArrayList<>();

        for (Long sellerId : activeSellerIds) {
            boolean exists = sellerDailyStatisticsRepository.existsBySellerIdAndStatDate(sellerId, yesterday);

            if (exists) { // 중복 검증 추가
                continue;
            }

            // seller 의 값이 없을 경우를 대비해서 검증 추가
            Optional<Seller> optionalSeller = sellerRepository.findById(sellerId);

            if (optionalSeller.isEmpty()) {
                continue;
            }

            Seller seller = optionalSeller.get();

            DailyStatsResponse dailyStatsResponse = orderItemRepository.aggregateStats(sellerId, start, end);

            //쿼리문에서 반환 값이 int 가 아닌 Long으로 지정 되어 null 검증 및 타입 변환 추가
            int totalOrders = dailyStatsResponse.totalOrders() != null ? dailyStatsResponse.totalOrders().intValue() : 0;
            int totalItems = dailyStatsResponse.totalItems() != null ? dailyStatsResponse.totalItems().intValue() : 0;
            int totalRefunds = dailyStatsResponse.totalRefunds() != null ? dailyStatsResponse.totalRefunds().intValue() : 0;
            BigDecimal totalSales = dailyStatsResponse.totalSales() != null ? dailyStatsResponse.totalSales() : BigDecimal.ZERO;
            BigDecimal refundAmount = dailyStatsResponse.refundAmount() != null ? dailyStatsResponse.refundAmount() : BigDecimal.ZERO;


            SellerDailyStatistics yesterdayStatistics = SellerDailyStatistics.of(
                    seller,
                    yesterday,
                    totalOrders,
                    totalItems,
                    totalRefunds,
                    totalSales,
                    refundAmount,
                    totalSales.subtract(refundAmount)
            );

            statisticsList.add(yesterdayStatistics);
        }

        if (!statisticsList.isEmpty()) {
            sellerDailyStatisticsRepository.saveAll(statisticsList);
        }
    }
}
