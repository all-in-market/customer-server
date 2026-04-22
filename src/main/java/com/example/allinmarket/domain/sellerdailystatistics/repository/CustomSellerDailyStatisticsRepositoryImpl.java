package com.example.allinmarket.domain.sellerdailystatistics.repository;

import com.example.allinmarket.domain.sellerdailystatistics.entity.QSellerDailyStatistics;
import com.example.allinmarket.seller.dailystatistics.dto.DailyStatisticsResponse;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RequiredArgsConstructor
public class CustomSellerDailyStatisticsRepositoryImpl implements CustomSellerDailyStatisticsRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public DailyStatisticsResponse findRangedStatistics(Long sellerId, LocalDate from, LocalDate to) {
        QSellerDailyStatistics s = QSellerDailyStatistics.sellerDailyStatistics;

        // Expressions.constant(null)은 QueryDSL 5.x에서 NPE를 던지므로 from/to는 Tuple로 집계 후 Java에서 직접 바인딩한다
        Tuple row = queryFactory
                .select(
                        s.seller.id,
                        s.totalOrders.sum(),
                        s.totalItems.sum(),
                        s.totalSales.sum(),
                        s.totalRefunds.sum(),
                        s.refundAmount.sum(),
                        s.netSales.sum()
                )
                .from(s)
                .where(
                        s.seller.id.eq(sellerId),
                        from != null ? s.statDate.goe(from) : null,
                        to != null ? s.statDate.loe(to) : null
                )
                .groupBy(s.seller.id)
                .fetchOne();

        if (row == null) return null;

        return new DailyStatisticsResponse(
                row.get(s.seller.id),
                from,
                to,
                row.get(s.totalOrders.sum()),
                row.get(s.totalItems.sum()),
                row.get(s.totalSales.sum()),
                row.get(s.totalRefunds.sum()),
                row.get(s.refundAmount.sum()),
                row.get(s.netSales.sum())
        );
    }
}
