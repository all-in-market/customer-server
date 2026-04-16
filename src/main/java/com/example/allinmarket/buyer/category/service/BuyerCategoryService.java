package com.example.allinmarket.buyer.category.service;

import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerCategoryService {
    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public PageResponse<CategoryDetailResponse> findAllCategory(Pageable pageable) {
        if (pageable.getPageNumber() == 0) {
            String key = "categories:" + pageable.getPageNumber() + ":" + pageable.getPageSize() + ":" + pageable.getSort();

            Object cachedObject = redisTemplate.opsForValue().get(key);

            if (cachedObject instanceof PageResponse<?> cached) {

                return new PageImpl<>(
                        (List<CategoryDetailResponse>) cached.content(),
                        pageable,
                        cached.totalElements()
                );
            }

            Page<Category> categories = categoryRepository..findAllVisibleProducts(pageable);

            Page<ProductDetailResponse> responses = products.map(ProductDetailResponse::from);

            PageResponse<ProductDetailResponse> pageResponse = PageResponse.register(responses);

            redisTemplate.opsForValue().set(key, pageResponse, Duration.ofMinutes(10));

            return responses;
        }

        Page<Category> categories = categoryRepository.findAll(
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "sortOrder") // sortOrder 기준 오름차순 정렬
                )
        );

        return PageResponse.register(categories.map(CategoryDetailResponse::from));
    }
}
