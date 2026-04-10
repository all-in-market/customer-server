package com.example.allinmarket.seller.product.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.product.dto.request.SellerProductCreateRequest;
import com.example.allinmarket.seller.repository.SellerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SellerProductService {
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductDetailResponse create(@Valid SellerProductCreateRequest request) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        Seller seller = sellerRepository.findById(sellerId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
        );
        Category category = categoryRepository.findById(request.categoryId()).orElseThrow(
                () -> new BaseException(ErrorEnum.CATEGORY_NOT_FOUND)
        );

        Product product = Product.of(
                seller,
                category,
                request.name(),
                request.price(),
                request.stock(),
                request.description()
        );

        Product savedProduct = productRepository.save(product);

        return ProductDetailResponse.from(savedProduct);
    }
}
