package com.example.allinmarket.domain.orderitem.repository;

import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
