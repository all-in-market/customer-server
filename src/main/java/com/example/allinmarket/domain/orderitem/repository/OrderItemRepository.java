package com.example.allinmarket.domain.orderitem.repository;

import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.seller.dailystatistics.dto.SalesStats;
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
            SELECT oi FROM OrderItem oi JOIN FETCH oi.seller WHERE oi.order.id = :orderId
            """)
    List<OrderItem> findAllByOrderIdWithSeller(@Param("orderId") Long orderId);

    @Query("""
            SELECT new com.example.allinmarket.seller.dailystatistics.dto.SalesStats(
            COUNT(DISTINCT o.id),
            SUM(oi.quantity),
            CAST(SUM(oi.unitPrice * oi.quantity) AS BIGDECIMAL))
            FROM OrderItem oi JOIN oi.order o JOIN Payment p ON p.order = o WHERE oi.seller.id = :sellerId
            AND p.status = com.example.allinmarket.domain.payment.enums.PaymentStatus.SUCCESS AND p.paidAt >= :start AND p.paidAt < :end
            """)
    SalesStats aggregateSalesStats(@Param("sellerId") Long sellerId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product WHERE oi.order.id = :orderId")
    List<OrderItem> findAllByOrderIdWithProduct(@Param("orderId") Long orderId);
}
