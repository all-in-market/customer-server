package com.example.allinmarket.domain.cartitem.repository;

import com.example.allinmarket.domain.cartitem.entity.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
        WHERE ci.id IN :cartItemIds
    """)
    List<CartItem> findAllByIdsWithCartAndProduct(@Param("cartItemIds") List<Long> cartItemIds);

    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.status != 'HIDDEN' AND ci.product.deletedAt IS NULL")
    Page<CartItem> findByCartId(Long cartId, Pageable pageable);

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
