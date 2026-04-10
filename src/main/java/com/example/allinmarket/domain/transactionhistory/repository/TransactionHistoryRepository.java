package com.example.allinmarket.domain.transactionhistory.repository;

import com.example.allinmarket.domain.transactionhistory.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
}
