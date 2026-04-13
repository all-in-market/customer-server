package com.example.allinmarket.seller.product.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.product.dto.request.SellerProductCreateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductStockUpdateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductUpdateRequest;
import com.example.allinmarket.seller.repository.SellerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProductService {

    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductDetailResponse create(Long sellerId, SellerProductCreateRequest request) {

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

    public PageResponse<ProductDetailResponse> findAll(Long sellerId, Pageable pageable) {
        return PageResponse.register(
                productRepository.findAllBySellerIdAndDeletedAtIsNull(sellerId, pageable)
                        .map(ProductDetailResponse::from)
        );
    }

    @Transactional
    public ProductDetailResponse update(Long sellerId, Long productId, SellerProductUpdateRequest request) {

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

        if (StringUtils.hasText(request.name())) {
            product.updateName(request.name());
        }

        if (request.price() != null) {
            product.updatePrice(request.price());
        }

        if (request.status() != null) {
            product.updateStatus(request.status());
        }

        if (StringUtils.hasText(request.description())) {
            product.updateDescription(request.description());
        }

        return ProductDetailResponse.from(product);
    }

    @Transactional
    public ProductDetailResponse delete(Long sellerId, Long productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId).orElseThrow(
                () -> new BaseException(ErrorEnum.PRODUCT_NOT_FOUND)
        );

        validationForbidden(sellerId, product);

        product.delete();

        return ProductDetailResponse.from(product);
    }

    @Transactional
    public ProductDetailResponse stockUpdate(Long sellerId, Long productId, SellerProductStockUpdateRequest request) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId).orElseThrow(
                () -> new BaseException(ErrorEnum.PRODUCT_NOT_FOUND)
        );

        validationForbidden(sellerId, product);

        product.updateStock(request.stock());

        return ProductDetailResponse.from(product);
    }

    private void validationForbidden(Long sellerId, Product product) {
        if(!product.getSeller().getId().equals(sellerId)) {
            throw new BaseException(ErrorEnum.FORBIDDEN);
        }
    }
}
