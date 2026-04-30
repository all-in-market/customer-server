package com.example.allinmarket.domain.restocksubscription.entity;

import com.example.allinmarket.common.entity.ModifiableEntity;
import com.example.allinmarket.domain.restocksubscription.enums.SubscriptionStatusEnum;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "restock_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestockSubscription extends ModifiableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long productId;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatusEnum status;

    private LocalDateTime lastNotifiedAt;

    public static RestockSubscription of(Long userId, Long productId) {
        RestockSubscription restockSubscription = new RestockSubscription();
        restockSubscription.userId = userId;
        restockSubscription.productId = productId;
        restockSubscription.status = SubscriptionStatusEnum.ACTIVE;

        return restockSubscription;
    }

    public void send() {
        this.status = SubscriptionStatusEnum.SENT;
        this.lastNotifiedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = SubscriptionStatusEnum.CANCELLED;
    }

    public void expire() {
        this.status = SubscriptionStatusEnum.EXPIRED;
    }

}
