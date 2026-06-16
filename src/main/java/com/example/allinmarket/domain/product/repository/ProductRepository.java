package com.example.allinmarket.domain.product.repository;

import com.example.allinmarket.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByIdAndDeletedAtIsNull(Long productId);

    @Query("SELECT p FROM Product p WHERE p.status != 'HIDDEN' AND p.deletedAt IS NULL")
    Page<Product> findAllVisibleProducts(Pageable pageable);

    @Query("""
         SELECT p
         FROM Product p
         JOIN FETCH p.seller
         WHERE p.id In :productIds
          AND p.status = 'ON_SALE'
          AND p.deletedAt IS NULL
    """)
    List<Product> findAllByIdInWithSeller(@Param("productIds") List<Long> productIds);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Product p
        SET p.stock = p.stock - :quantity
        WHERE p.id = :productId
          AND p.stock >= :quantity
          AND p.status = 'ON_SALE'
          AND p.deletedAt IS NULL
    """)
    int decreaseStockIfEnough(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Query("SELECT p FROM Product p WHERE p.id = :productId AND p.status != 'HIDDEN' AND p.deletedAt IS NULL")
    Optional<Product> findVisibleProductById(Long productId);

    Page<Product> findAllBySellerIdAndDeletedAtIsNull(Long sellerId, Pageable pageable);


    // LIKE로 인해 풀 테이블 스캔 발생 (PostgreSQL의 Full Text Search | Elasticsearch 도입을 고려.) or 가능한 RAG를 사용해서 유사도 검색으로 전환
    @Query("""
        SELECT p FROM Product p 
        WHERE (p.name LIKE %:keyword% OR p.description LIKE %:keyword%)
        AND p.status != 'HIDDEN' 
        AND p.deletedAt IS NULL
        """)
    Page<Product> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
