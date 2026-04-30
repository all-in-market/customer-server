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
import java.util.Optional;


public interface PayoutRepository extends JpaRepository<Payout, Long> {

    @Query("SELECT p FROM Payout p WHERE p.status = :status AND p.retryCount < :retryCount AND p.id > :lastId ORDER BY p.id ASC ")
    List<Payout> findBatch(@Param("status") PayoutStatus status,
                               @Param("retryCount") int retryCount,
                               @Param("lastId") Long lastId,
                               Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payout p WHERE p.id = :payoutId")
    Optional<Payout> findByIdForUpdate(@Param("payoutId") Long payoutId);
}
