package com.example.allinmarket.domain.refund.repository;

import com.example.allinmarket.domain.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund,Long> {
}
