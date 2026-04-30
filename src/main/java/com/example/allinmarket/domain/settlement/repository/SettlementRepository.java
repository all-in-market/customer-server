package com.example.allinmarket.domain.settlement.repository;

import com.example.allinmarket.domain.settlement.entity.Settlement;
import com.example.allinmarket.domain.settlement.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Page<Settlement> findAllBySellerId(Long sellerId, Pageable pageable);

    @Query("SELECT s FROM Settlement s WHERE s.status = :status AND NOT EXISTS (SELECT 1 FROM Payout p WHERE p.settlementId = s.id) AND s.id > :lastId ORDER BY s.id ASC ")
    List<Settlement> findWithoutPayout(@Param("status") SettlementStatus status, @Param("lastId") Long lastId, Pageable pageable);
}
