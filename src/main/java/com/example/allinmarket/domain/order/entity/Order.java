package com.example.allinmarket.domain.order.entity;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.common.entity.ModifiableEntity;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends ModifiableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    @PositiveOrZero
    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String recipient;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String phone;

    @NotBlank
    @Column(nullable = false)
    private String address;

    public static Order of(Buyer buyer, BigDecimal totalAmount, String trackingNumber, String recipient, String phone, String address) {
        Order order = new Order();
        order.buyer = buyer;
        order.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        order.status = OrderStatus.CREATED;
        order.trackingNumber = trackingNumber;
        order.recipient = recipient;
        order.phone = phone;
        order.address = address;
        return order;
    }
}
