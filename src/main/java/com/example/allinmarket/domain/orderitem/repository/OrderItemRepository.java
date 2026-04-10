package com.example.allinmarket.domain.orderitem.repository;

import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
