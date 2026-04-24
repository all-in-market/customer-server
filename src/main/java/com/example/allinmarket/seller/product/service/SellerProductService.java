package com.example.allinmarket.seller.product.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.product.dto.request.SellerProductCreateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductStockUpdateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductUpdateRequest;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProductService {

    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

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
        if (pageable.getPageNumber() < 2) {
            String key = "sellerProducts:" + sellerId + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize() + ":" + pageable.getSort();

            Object cachedObject = redisTemplate.opsForValue().get(key);

            if (cachedObject instanceof PageResponse<?> cached) {
                return (PageResponse<ProductDetailResponse>) cached;
            }

            Page<Product> products = productRepository.findAllBySellerIdAndDeletedAtIsNull(sellerId, pageable);

            Page<ProductDetailResponse> responses = products.map(ProductDetailResponse::from);

            PageResponse<ProductDetailResponse> pageResponse = PageResponse.register(responses);

            redisTemplate.opsForValue().set(key, pageResponse, Duration.ofMinutes(5));

            return pageResponse;
        }

        Page<Product> products = productRepository.findAllBySellerIdAndDeletedAtIsNull(sellerId, pageable);

        return PageResponse.register(
                products.map(ProductDetailResponse::from)
        );
    }

    @Transactional
    public ProductDetailResponse update(Long sellerId, Long productId, SellerProductUpdateRequest request) {

        boolean changed = false;

        Product product = productRepository.findByIdAndDeletedAtIsNull(productId).orElseThrow(
                () -> new BaseException(ErrorEnum.PRODUCT_NOT_FOUND)
        );

        validationForbidden(sellerId, product);

        if(request.categoryId() != null) {
            Category category = categoryRepository.findByIdAndDeletedAtIsNull(request.categoryId()).orElseThrow(
                    () -> new BaseException(ErrorEnum.CATEGORY_NOT_FOUND)
            );
            product.updateCategory(category);
            changed = true;
        }

        if (StringUtils.hasText(request.name())) {
            product.updateName(request.name());
            changed = true;
        }

        if (request.price() != null) {
            product.updatePrice(request.price());
            changed = true;
        }

        if (request.status() != null) {
            product.updateStatus(request.status());
            changed = true;
        }

        if (StringUtils.hasText(request.description())) {
            product.updateDescription(request.description());
            changed = true;
        }

        if(changed) {
            evictSearchProductCache(null);
            evictSearchProductCache(sellerId);
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

        // 상품 삭제 시 캐시 삭제
        evictSearchProductCache(null);
        evictSearchProductCache(sellerId);

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

    public void evictSearchProductCache(Long sellerId) {

        String pattern = sellerId == null ? "products:search:*" : "sellerProducts:" + sellerId + ":*";

        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();

        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();

        try (Cursor<byte[]> cursor = connection.scan(options)) {

            List<byte[]> batch = new ArrayList<>();

            while (cursor.hasNext()) {
                batch.add(cursor.next());

                if (batch.size() >= 100) {
                    connection.del(batch.toArray(new byte[0][]));
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                connection.del(batch.toArray(new byte[0][]));
            }

        } finally {
            connection.close();
        }
    }

    private void validationForbidden(Long sellerId, Product product) {
        if(!product.getSeller().getId().equals(sellerId)) {
            throw new BaseException(ErrorEnum.FORBIDDEN);
        }
    }
}
