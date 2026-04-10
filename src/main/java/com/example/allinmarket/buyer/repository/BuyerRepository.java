package com.example.allinmarket.buyer.repository;

import com.example.allinmarket.buyer.entity.Buyer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerRepository extends JpaRepository<Buyer, Long> {
}
