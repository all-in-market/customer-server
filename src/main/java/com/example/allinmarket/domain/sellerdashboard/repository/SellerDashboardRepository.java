package com.example.allinmarket.domain.sellerdashboard.repository;

import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SellerDashboardRepository extends JpaRepository<SellerDashboard, Long> {
    Optional<SellerDashboard> findBySellerIdAndStatDate(Long id, LocalDate day);
}
