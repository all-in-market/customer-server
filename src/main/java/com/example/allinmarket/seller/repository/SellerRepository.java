package com.example.allinmarket.seller.repository;

import com.example.allinmarket.seller.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    boolean existsByEmail(String email);
}
