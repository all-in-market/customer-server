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

        List<Long> activeSellerIds = orderItemRepository.findActiveSellerIds(start, end);

        List<SellerDailyStatistics> statisticsList = new ArrayList<>();

        for (Long sellerId : activeSellerIds) {
            boolean exists = sellerDailyStatisticsRepository.existsBySellerIdAndStatDate(sellerId, yesterday);

            if (exists) {
                continue;
            }

            Optional<Seller> optionalSeller = sellerRepository.findById(sellerId);

            if (optionalSeller.isEmpty()) {
                continue;
            }

            Seller seller = optionalSeller.get();

            DailyStatsResponse dailyStatsResponse = orderItemRepository.aggregateStats(sellerId, start, end);

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
