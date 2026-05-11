package com.example.allinmarket.seller.product.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.entity.ProductImage;
import com.example.allinmarket.domain.product.repository.ProductImageRepository;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.domain.restocksubscription.event.RestockEvent;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.product.dto.request.SellerProductCreateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductStockUpdateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductUpdateRequest;
import com.example.allinmarket.seller.product.dto.response.ProductImageDetailResponse;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProductService {

    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private final S3UploadService s3UploadService;

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

        evictSellerSearchProductCacheAfterCommit(sellerId);

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
            evictSearchProductCacheAfterCommit(sellerId);
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
        evictSearchProductCacheAfterCommit(sellerId);

        return ProductDetailResponse.from(product);
    }

    @Transactional
    public ProductDetailResponse stockUpdate(Long sellerId, Long productId, SellerProductStockUpdateRequest request) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId).orElseThrow(
                () -> new BaseException(ErrorEnum.PRODUCT_NOT_FOUND)
        );

        validationForbidden(sellerId, product);

        boolean wasOutOfStock = product.getStock() == 0;
        product.updateStock(request.stock());

        if(wasOutOfStock && product.getStock() > 0) {
            eventPublisher.publishEvent(new RestockEvent(productId));
        }

        evictSellerSearchProductCacheAfterCommit(sellerId);

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

    @Transactional
    public ProductImageDetailResponse uploadProductImage(
            Long sellerId,
            Long productId,
            MultipartFile image,
            Integer sortOrder,
            boolean representative
    ) {
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new BaseException(ErrorEnum.PRODUCT_NOT_FOUND)
        );

        validateProductOwner(product, sellerId);

        String imageUrl = s3UploadService.uploadProductImage(image, productId);

        ProductImage productImage = ProductImage.of(
                product,
                imageUrl,
                sortOrder,
                representative
        );

        ProductImage savedImage = productImageRepository.save(productImage);

        return ProductImageDetailResponse.from(savedImage);
    }

    public List<ProductImageDetailResponse> getProductImages(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BaseException(ErrorEnum.PRODUCT_NOT_FOUND));

        List<ProductImage> productImages =
                productImageRepository.findByProductIdOrderByRepresentativeDescSortOrderAsc(product.getId());

        return productImages.stream()
                .map(ProductImageDetailResponse::from)
                .toList();
    }

    private void validateProductOwner(Product product, Long sellerId) {
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BaseException(ErrorEnum.PRODUCT_ACCESS_DENIED);
        }
    }

    private void evictSearchProductCacheAfterCommit(Long sellerId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                    @Override
                    public void afterCommit() {
                        evictSearchProductCache(null);
                        evictSearchProductCache(sellerId);
                    }
                });
    }

    private void evictSellerSearchProductCacheAfterCommit(Long sellerId) {
        registerSynchronization(new TransactionSynchronization(){
            @Override
            public void afterCommit() {
                evictSearchProductCache(sellerId);
            }
        });
    }

    private void validationForbidden(Long sellerId, Product product) {
        if(!product.getSeller().getId().equals(sellerId)) {
            throw new BaseException(ErrorEnum.FORBIDDEN);
        }
    }


}
