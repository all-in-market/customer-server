package com.example.allinmarket.domain.product.repository;

import com.example.allinmarket.domain.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderByRepresentativeDescSortOrderAsc(Long productId);
}
