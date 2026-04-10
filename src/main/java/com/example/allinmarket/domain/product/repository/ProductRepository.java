package com.example.allinmarket.domain.product.repository;

import com.example.allinmarket.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p WHERE p.status != 'HIDDEN'")
    Page<Product> findAllVisibleProducts(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
         SELECT p
         FROM Product p
         JOIN FETCH Seller s
         WHERE p.id In :productIds
    """)
    List<Product> findAllByIdInWithSellerWithLock(@Param("productIds") List<Long> productIds);
}
