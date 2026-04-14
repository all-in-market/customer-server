package com.example.allinmarket.domain.refund.repository;

import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.refund.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund,Long> {
    Optional<Refund> findByPayment(Payment payment);
    Page<Refund> findAllByBuyerId(Long buyerId, Pageable pageable);
}
