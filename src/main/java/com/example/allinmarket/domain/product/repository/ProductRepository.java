package com.example.allinmarket.domain.product.repository;

import com.example.allinmarket.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p WHERE p.status != 'HIDDEN' AND p.deletedAt IS NULL")
    Page<Product> findAllVisibleProducts(Pageable pageable);
}
