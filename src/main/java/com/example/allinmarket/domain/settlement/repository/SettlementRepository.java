package com.example.allinmarket.domain.settlement.repository;

import com.example.allinmarket.domain.settlement.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Page<Settlement> findAllBySellerId(Long sellerId, Pageable pageable);

    boolean existsBySellerIdAndPeriodStartAndPeriodEnd(Long sellerId, LocalDate start, LocalDate end);
}
