package com.example.allinmarket.buyer.product.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerProductService {
    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public Page<ProductDetailResponse> findAllProducts(Pageable pageable) {
        if (pageable.getPageNumber() < 10) {
            String key = "products:search:" + pageable.getPageNumber() + ":" + pageable.getPageSize() + ":" + pageable.getSort();

            Object cachedObject = redisTemplate.opsForValue().get(key);

            if (cachedObject instanceof PageResponse<?> cached) {

                return new PageImpl<>(
                        (List<ProductDetailResponse>) cached.content(),
                        pageable,
                        cached.totalElements()
                );
            }

            Page<Product> products = productRepository.findAllVisibleProducts(pageable);

            Page<ProductDetailResponse> responses = products.map(ProductDetailResponse::from);

            PageResponse<ProductDetailResponse> pageResponse = PageResponse.register(responses);

            redisTemplate.opsForValue().set(key, pageResponse, Duration.ofMinutes(10));

            return responses;
        }

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
