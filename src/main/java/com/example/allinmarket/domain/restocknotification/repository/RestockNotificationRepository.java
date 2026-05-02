package com.example.allinmarket.domain.restocknotification.repository;

import com.example.allinmarket.domain.restocknotification.entity.RestockNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestockNotificationRepository extends JpaRepository<RestockNotification, Long> {
}
