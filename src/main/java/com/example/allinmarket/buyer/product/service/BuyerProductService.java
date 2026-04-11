package com.example.allinmarket.buyer.product.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerProductService {
    private final ProductRepository productRepository;

    public Page<ProductDetailResponse> findAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAllVisibleProducts(pageable);

        return products.map(ProductDetailResponse::from);
    }

    public ProductDetailResponse findOneProduct(Long productId) {
        Product product = productRepository.findVisibleProductById(productId).orElseThrow(
                () -> new BaseException(ErrorEnum.PRODUCT_NOT_FOUND)
        );

        return ProductDetailResponse.from(product);
    }
}
