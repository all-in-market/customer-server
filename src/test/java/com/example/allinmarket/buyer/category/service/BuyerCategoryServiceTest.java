package com.example.allinmarket.buyer.category.service;

import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @Test
    void 카테고리_목록_조회_성공_테스트() {
        // given
        Category category = Category.of("전자제품", 1);

        Page<Category> page = new PageImpl<>(List.of(category));

        given(categoryRepository.findAll(any(Pageable.class))).willReturn(page);

        // when
        PageResponse<CategoryDetailResponse> response =
                buyerCategoryService.findAllCategory(PageRequest.of(0, 10));

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("전자제품");
    }

    @Test
    void 카테고리_목록_조회_실패_테스트() {
        // given
        given(categoryRepository.findAll(any(Pageable.class))).willReturn(Page.empty());

        // when
        PageResponse<CategoryDetailResponse> response =
                buyerCategoryService.findAllCategory(PageRequest.of(0, 10));

        // then
        assertThat(response.content()).isEmpty();
    }
}
