package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.refund.repository.RefundRepository;
import com.example.allinmarket.domain.sellerdailystatistics.entity.SellerDailyStatistics;
import com.example.allinmarket.domain.sellerdailystatistics.repository.SellerDailyStatisticsRepository;
import com.example.allinmarket.seller.dailystatistics.dto.RefundStats;
import com.example.allinmarket.seller.dailystatistics.dto.SalesStats;
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

@Component
@RequiredArgsConstructor
public class DailyStatisticsScheduler {

    private final SellerRepository sellerRepository;
    private final SellerDailyStatisticsRepository sellerDailyStatisticsRepository;
    private final OrderItemRepository orderItemRepository;
    private final RefundRepository refundRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void generateDailyStatistics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = yesterday.plusDays(1).atStartOfDay();

        // 무판매일에도 집계 0 보장을 위해 모든 판매자 조회
        List<Seller> sellers = sellerRepository.findAll();

        List<SellerDailyStatistics> statisticsList = new ArrayList<>();

        for (Seller seller : sellers) {

            Long sellerId = seller.getId();
            boolean exists = sellerDailyStatisticsRepository.existsBySellerIdAndStatDate(sellerId, yesterday);

            if (exists) { // 중복 검증 추가
                continue;
            }

            // 판매가 없으면 null 반환 위험
            SalesStats salesStats = orderItemRepository.aggregateSalesStats(sellerId, start, end);
            RefundStats refundStats = refundRepository.aggregateRefundStats(sellerId, start, end);

            //쿼리문에서 반환 값이 int 가 아닌 Long으로 지정 되어 null 검증 및 타입 변환 추가
            int totalOrders = salesStats.totalOrders() != null ? salesStats.totalOrders().intValue() : 0;
            int totalItems = salesStats.totalItems() != null ? salesStats.totalItems().intValue() : 0;
            int totalRefunds = refundStats.totalRefunds() != null ? refundStats.totalRefunds().intValue() : 0;
            BigDecimal totalSales = salesStats.totalSales() != null ? salesStats.totalSales() : BigDecimal.ZERO;
            BigDecimal refundAmount = refundStats.refundAmount() != null ? refundStats.refundAmount() : BigDecimal.ZERO;

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
