package com.example.allinmarket.domain.cartitem.repository;

import com.example.allinmarket.domain.cartitem.entity.CartItem;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("""
        SELECT ci FROM CartItem ci
        JOIN FETCH ci.cart c
        JOIN FETCH ci.product p
        WHERE ci.id IN :ids
    """)
    List<CartItem> findAllByIdsWithCartAndProduct(@Param("ids") List<Long> ids);

    @Query("SELECT p FROM Product p WHERE p.status != 'HIDDEN' AND p.deletedAt IS NULL")
    Page<CartItem> findByCartId(Long id, Pageable pageable);

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
