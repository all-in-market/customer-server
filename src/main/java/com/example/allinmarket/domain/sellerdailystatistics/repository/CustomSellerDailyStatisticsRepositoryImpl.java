package com.example.allinmarket.domain.sellerdailystatistics.repository;

import com.example.allinmarket.domain.sellerdailystatistics.dto.DailyStatisticsResponse;
import com.example.allinmarket.domain.sellerdailystatistics.entity.QSellerDailyStatistics;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RequiredArgsConstructor
public class CustomSellerDailyStatisticsRepositoryImpl implements CustomSellerDailyStatisticsRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public DailyStatisticsResponse findRangedStatistics(Long sellerId, LocalDate from, LocalDate to) {
        QSellerDailyStatistics s = QSellerDailyStatistics.sellerDailyStatistics;

        return queryFactory
                .select(Projections.constructor(DailyStatisticsResponse.class,
                        s.seller.id,
                        Expressions.constant(from),
                        Expressions.constant(to),
                        s.totalOrders.sum(),
                        s.totalItems.sum(),
                        s.totalSales.sum(),
                        s.totalRefunds.sum(),
                        s.refundAmount.sum(),
                        s.netSales.sum()
                ))
                .from(s)
                .where(
                        s.seller.id.eq(sellerId),
                        from != null ? s.statDate.goe(from) : null,
                        to != null ? s.statDate.loe(to) : null
                )
                .groupBy(s.seller.id)
                .fetchOne();
    }
}
