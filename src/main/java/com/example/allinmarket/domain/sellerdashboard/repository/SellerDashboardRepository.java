package com.example.allinmarket.domain.sellerdashboard.repository;

import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SellerDashboardRepository extends CrudRepository<SellerDashboard, Long> {
    Optional<SellerDashboard> findBySellerIdAndStatDate(Long id, LocalDate day);
}
