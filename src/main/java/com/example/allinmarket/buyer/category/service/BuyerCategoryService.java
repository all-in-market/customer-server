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
        PageRequest pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "sortOrder")
        );

        if (pageable.getPageNumber() == 0) {
            String key = "categories:" + pageable.getPageNumber() + ":" + pageable.getPageSize() + ":sortOrder:ASC";

            Object cachedObject = redisTemplate.opsForValue().get(key);

            if (cachedObject instanceof PageResponse<?> cached) {

                return (PageResponse<CategoryDetailResponse>) cached;
            }

            Page<Category> categories = categoryRepository.findAll(pageRequest);

            Page<CategoryDetailResponse> responses = categories.map(CategoryDetailResponse::from);

            PageResponse<CategoryDetailResponse> pageResponse = PageResponse.register(responses);

            redisTemplate.opsForValue().set(key, pageResponse, Duration.ofMinutes(10));

            return pageResponse;
        }

        Page<Category> categories = categoryRepository.findAll(pageRequest);

        return PageResponse.register(categories.map(CategoryDetailResponse::from));
    }
}
