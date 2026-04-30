package com.example.allinmarket.domain.payout.repository;

import com.example.allinmarket.domain.payout.entity.Payout;
import com.example.allinmarket.domain.payout.enums.PayoutStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface PayoutRepository extends JpaRepository<Payout, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payout p WHERE p.status = :status AND p.retryCount < :retryCount ORDER BY p.id ASC ")
    List<Payout> findForUpdate(@Param("status") PayoutStatus status,
                               @Param("retryCount") int retryCount,
                               Pageable pageable);
}
