package com.example.allinmarket.domain.payment.repository;

import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.lang.ScopedValue;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByOrderIdAndStatus(Long orderId, TransactionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p
        from Payment p
        join fetch p.order o
        where p.impUid = :paymentId
    """)
    Optional<Payment> findByImpUidWithOrderForUpdate(String paymentId);

    Optional<Payment> findByImpUid(String paymentId);
}
