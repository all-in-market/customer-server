package com.example.allinmarket.domain.refund.repository;

import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.seller.dailystatistics.dto.RefundStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund,Long> {
    Optional<Refund> findByPayment(Payment payment);
    Page<Refund> findAllByBuyerId(Long buyerId, Pageable pageable);

    Optional<Refund> findByIdAndBuyerId(Long refundId, Long currentUserId);

    // 현재는 전체 환불만 지원.
    // 부분 환불 도입 시 RefundItem 도입 후 환불 추적 필요
    @Query("""
            SELECT new com.example.allinmarket.seller.dailystatistics.dto.RefundStats(
            COUNT(DISTINCT r.id),
            CAST(SUM(oi.unitPrice * oi.quantity) AS BIGDECIMAL))
            FROM Refund r JOIN r.payment p JOIN p.order o JOIN OrderItem oi ON oi.order = o
            WHERE oi.seller.id = :sellerId AND r.status = com.example.allinmarket.domain.refund.enums.RefundStatus.SUCCESS
            AND r.processedAt >= :start AND r.processedAt < :end
            """)
    RefundStats aggregateRefundStats(@Param("sellerId") Long sellerId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);
}
