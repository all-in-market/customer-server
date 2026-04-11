package com.example.allinmarket.buyer.repository;

import com.example.allinmarket.buyer.entity.Buyer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.ScopedValue;
import java.util.Optional;

public interface BuyerRepository extends JpaRepository<Buyer, Long> {
    boolean existsByEmail(String email);

    Optional<Buyer> findByEmail(String email);

    Optional<Buyer> findByIdAndDeletedAtIsNull(Long buyerId);
}
