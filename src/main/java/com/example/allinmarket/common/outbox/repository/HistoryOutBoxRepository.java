package com.example.allinmarket.common.outbox.repository;

import com.example.allinmarket.common.outbox.entity.HistoryOutBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistoryOutBoxRepository extends JpaRepository<HistoryOutBox, Long> {

    @Query("SELECT h FROM HistoryOutBox h WHERE h.processed = false AND h.retryCount < :maxRetryCount")
    List<HistoryOutBox> findUnprocessed(@Param("maxRetryCount") int maxRetryCount);
}
