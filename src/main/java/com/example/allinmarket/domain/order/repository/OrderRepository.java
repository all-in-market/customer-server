package com.example.allinmarket.domain.order.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.lang.ScopedValue;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Page<Order> findByBuyerId(Long buyerId, Pageable pageable);

    Page<Order> findByBuyerIdAndStatus(Long buyerId, OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndBuyerId(Long orderId, Long buyerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o
        from Order o
        where o.id = :orderId
          and o.buyer.id = :buyerId
    """)
    Optional<Order> findByIdAndBuyerIdWithLock(@Param("orderId") Long orderId, @Param("buyerId") Long buyerId);
}
