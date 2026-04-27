package com.example.allinmarket.common.outbox.repository;

import com.example.allinmarket.common.outbox.entity.DashboardOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;


import java.util.List;

public interface DashboardOutboxRepository extends JpaRepository<DashboardOutbox, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM DashboardOutbox o WHERE o.processed = false ORDER BY o.id ASC")
    List<DashboardOutbox> findTop100ForUpdate(Pageable pageable);
}
