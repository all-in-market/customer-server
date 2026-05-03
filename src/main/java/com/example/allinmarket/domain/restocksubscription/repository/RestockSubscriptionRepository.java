package com.example.allinmarket.domain.restocksubscription.repository;

import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import com.example.allinmarket.domain.restocksubscription.enums.SubscriptionStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestockSubscriptionRepository extends JpaRepository<RestockSubscription, Long> {

    Optional<RestockSubscription> findByUserIdAndProductId(Long userId, Long productId);

    Page<RestockSubscription> findByUserIdAndStatus(Long userId, SubscriptionStatusEnum status, Pageable pageable);
}
