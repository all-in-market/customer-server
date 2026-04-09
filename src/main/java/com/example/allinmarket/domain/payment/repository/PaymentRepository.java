package com.example.allinmarket.domain.payment.repository;

import com.example.allinmarket.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
