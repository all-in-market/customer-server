package com.example.allinmarket.buyer.category.service;

import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class BuyerCategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BuyerCategoryService buyerCategoryService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void 카테고리_목록_조회_성공_테스트() {
        // given
        Category category = Category.of("전자제품", 1);

        ReflectionTestUtils.setField(category, "id", 1L);

        List<Category> categories = List.of(category);

        given(categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"))).willReturn(categories);

        given(redisTemplate.opsForValue()).willReturn(Mockito.mock(ValueOperations.class));

        // when
        List<CategoryDetailResponse> response = buyerCategoryService.findAllCategory();

        // then
        assertThat(response).isNotNull();
        assertThat(response.get(0).id()).isEqualTo(1L);
        assertThat(response.get(0).name()).isEqualTo("전자제품");
    }

    @Test
    void 카테고리_목록_조회_실패_테스트() {
        // given
        given(categoryRepository.findAll(any(Sort.class))).willReturn(Collections.emptyList());

        given(redisTemplate.opsForValue()).willReturn(Mockito.mock(ValueOperations.class));

        // when
        List<CategoryDetailResponse> response = buyerCategoryService.findAllCategory();

        // then
        assertThat(response.isEmpty());
    }
}
