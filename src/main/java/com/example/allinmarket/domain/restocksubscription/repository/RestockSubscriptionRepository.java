package com.example.allinmarket.domain.restocksubscription.repository;

import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestockSubscriptionRepository extends JpaRepository<RestockSubscription, Long> {
}
