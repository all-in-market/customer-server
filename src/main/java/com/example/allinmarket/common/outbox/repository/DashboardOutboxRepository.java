package com.example.allinmarket.common.outbox.repository;

import com.example.allinmarket.common.outbox.entity.DashboardOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DashboardOutboxRepository extends JpaRepository<DashboardOutbox, Long> {

    @Query("SELECT o.id FROM DashboardOutbox o WHERE o.processed = false AND o.retryCount < :maxRetryCount ORDER BY o.id")
    List<Long> findUnprocessedIds(@Param("maxRetryCount") int maxRetryCount, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM DashboardOutbox o WHERE o.id = :outboxId")
    Optional<DashboardOutbox> findByIdForUpdate(@Param("outboxId") Long outboxId);
}
