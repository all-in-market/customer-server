package com.example.allinmarket.domain.orderitem.repository;

import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.seller.dailystatistics.dto.DailyStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("""
    SELECT oi FROM OrderItem oi
    JOIN FETCH oi.order o
    WHERE oi.seller.id = :sellerId
    """)
    Page<OrderItem> findAllBySellerId(Long sellerId, Pageable pageable);

    @Query("""
    SELECT oi FROM OrderItem oi
    JOIN FETCH oi.order o
    WHERE o.id = :orderId
    AND o.buyer.id = :buyerId
    """)
    List<OrderItem> findAllByBuyerIdAndOrderId(Long buyerId, Long orderId);

    @Query("""
    SELECT new com.example.allinmarket.seller.dailystatistics.dto.DailyStatsResponse(
    COUNT(DISTINCT oi.order.id), SUM(oi.quantity),
    SUM(CASE WHEN oi.order.status = com.example.allinmarket.domain.order.enums.OrderStatus.REFUNDED THEN 1L ELSE 0L END),
    CAST(SUM(oi.unitPrice * oi.quantity) AS BIGDECIMAL),
    CAST(SUM(CASE WHEN oi.order.status = com.example.allinmarket.domain.order.enums.OrderStatus.REFUNDED THEN oi.unitPrice * oi.quantity ELSE 0 END)AS BIGDECIMAL))
    FROM OrderItem oi WHERE oi.seller.id = :sellerId AND oi.order.createdAt >= :start AND oi.order.createdAt < :end
    """)
    DailyStatsResponse aggregateStats(@Param("sellerId") Long sellerId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Query("SELECT DISTINCT oi.seller.id FROM OrderItem oi JOIN oi.seller s WHERE oi.order.createdAt >= :start AND oi.order.createdAt < :end")
    List<Long> findActiveSellerIds(LocalDateTime start, LocalDateTime end);
}
