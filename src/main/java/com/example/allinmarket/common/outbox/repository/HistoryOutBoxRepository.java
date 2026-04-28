package com.example.allinmarket.common.outbox.repository;

import com.example.allinmarket.common.outbox.entity.HistoryOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HistoryOutBoxRepository extends JpaRepository<HistoryOutbox, Long> {

    @Query("SELECT h FROM HistoryOutbox h WHERE h.processed = false AND h.retryCount < :maxRetryCount ORDER BY h.id")
    List<HistoryOutbox> findUnprocessed(@Param("maxRetryCount") int maxRetryCount, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HistoryOutbox h WHERE h.id = :outBoxId")
    Optional<HistoryOutbox> findByIdForUpdate(@Param("outBoxId") Long outBoxId);
}
