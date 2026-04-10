package com.example.allinmarket.domain.orderitem.entity;

import com.example.allinmarket.common.entity.ModifiableEntity;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.seller.entity.Seller;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order_items")
public class OrderItem extends ModifiableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @NotBlank
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @PositiveOrZero
    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @PositiveOrZero
    private int quantity;

    public static OrderItem of(Order order, Product product, Seller seller, String productName, BigDecimal unitPrice, int quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.order = order;
        orderItem.product = product;
        orderItem.seller = seller;
        orderItem.productName = productName;
        orderItem.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        orderItem.quantity = quantity;
        return orderItem;
    }
}
