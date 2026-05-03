package com.example.allinmarket.domain.restocknotification.repository;

import com.example.allinmarket.domain.restocknotification.entity.RestockNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RestockNotificationRepository extends JpaRepository<RestockNotification, Long> {

    @Query("SELECT r FROM RestockNotification r WHERE r.userId = :userId AND r.isRead = false")
    Page<RestockNotification> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT r FROM RestockNotification r WHERE r.userId = :userId AND r.productId = :productId")
    Optional<RestockNotification> findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
