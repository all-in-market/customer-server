package com.example.allinmarket.domain.order.entity;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.common.entity.BaseEntity;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    @NotNull
    @PositiveOrZero
    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @NotBlank
    @Length(max = 50)
    private String trackingNumber;

    @NotBlank
    @Length(max = 50)
    private String recipient;

    @NotBlank
    @Length(max = 20)
    private String phone;

    @NotBlank
    @Length(max = 255)
    private String address;

    public static Order of(Buyer buyer, BigDecimal totalAmount, OrderStatus status, String trackingNumber, String recipient, String phone, String address) {
        Order order = new Order();
        order.buyer = buyer;
        order.totalAmount = totalAmount;
        order.status = status;
        order.trackingNumber = trackingNumber;
        order.recipient = recipient;
        order.phone = phone;
        order.address = address;
        return order;
    }

}
