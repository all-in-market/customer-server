package com.example.allinmarket.buyer.category.service;

import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
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

    public List<CategoryDetailResponse> findAllCategory() {
        String key = "categories:all";

        Object cachedObject = redisTemplate.opsForValue().get(key);

        if (cachedObject instanceof List<?> cached) {

            return (List<CategoryDetailResponse>) cached;
        }

        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"));

        List<CategoryDetailResponse> responses = categories.stream()
                .map(CategoryDetailResponse::from)
                .toList();

        redisTemplate.opsForValue().set(key, responses, Duration.ofMinutes(10));

        return responses;
    }
}
