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
import com.example.allinmarket.seller.product.dto.request.SellerProductUpdateRequest;
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
        Seller seller = sellerRepository.findByIdAndDeletedAtIsNull(sellerId).orElseThrow(
                () -> new BaseException(ErrorEnum.SELLER_NOT_FOUND)
        );
        Category category = categoryRepository.findByIdAndDeletedAtIsNull(request.categoryId()).orElseThrow(
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

    public ProductDetailResponse update(Long sellerId, Long productId, @Valid SellerProductUpdateRequest request) {

        Product product = productRepository.findByIdAndDeletedAtIsNull(productId).orElseThrow(
                () -> new BaseException(ErrorEnum.PRODUCT_NOT_FOUND)
        );

        validationForbidden(sellerId, product);

        if(request.categoryId() != null) {
            Category category = categoryRepository.findByIdAndDeletedAtIsNull(request.categoryId()).orElseThrow(
                    () -> new BaseException(ErrorEnum.CATEGORY_NOT_FOUND)
            );
            product.updateCategory(category);
        }

        if (request.name() != null && !request.name().isBlank()) {
            product.updateName(request.name());
        }

        if (request.price() != null) {
            product.updatePrice(request.price());
        }

        if (request.status() != null) {
            product.updateStatus(request.status());
        }

        if (request.description() != null && !request.description().isBlank()) {
            product.updateDescription(request.description());
        }

        return ProductDetailResponse.from(product);
    }

    private void validationForbidden(Long sellerId, Product product) {
        if(!product.getSeller().getId().equals(sellerId)) {
            throw new BaseException(ErrorEnum.FORBIDDEN);
        }
    }
}
