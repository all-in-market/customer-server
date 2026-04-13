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
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByIdAndDeletedAtIsNull(Long productId);

    @Query("SELECT p FROM Product p WHERE p.status != 'HIDDEN' AND p.deletedAt IS NULL")
    Page<Product> findAllVisibleProducts(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
         SELECT p
         FROM Product p
         JOIN FETCH Seller s
         WHERE p.id In :productIds
    """)
    List<Product> findAllByIdInWithSellerWithLock(@Param("productIds") List<Long> productIds);

    @Query("SELECT p FROM Product p WHERE p.status != 'HIDDEN' AND p.deletedAt IS NULL")
    Optional<Product> findVisibleProductById(Long productId);

    Page<Product> findAllBySellerIdAndDeletedAtIsNull(Long sellerId, Pageable pageable);
}
