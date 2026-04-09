package com.example.allinmarket.buyer.entity.product.service;

import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BuyerProductService {
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductDetailResponse> findAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAllVisibleProducts(pageable);

        return products.map(ProductDetailResponse::from);
    }
}
