package com.example.allinmarket.buyer.category.controller;

import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.buyer.category.service.BuyerCategoryService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtAuthenticationFilter;
import com.example.allinmarket.common.security.LoginRateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

@WebMvcTest(BuyerCategoryController.class)
@AutoConfigureRestTestClient
public class BuyerCategoryControllerTest {
    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private LoginRateLimitFilter loginRateLimitFilter;

    @MockitoBean
    private BuyerCategoryService buyerCategoryService;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(loginRateLimitFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    void 카테고리_목록_조회_성공_테스트() {
        // given
        CategoryDetailResponse response = new CategoryDetailResponse(1L, "전자제품", 1);

        List<CategoryDetailResponse> responseList = List.of(response);

        given(buyerCategoryService.findAllCategory()).willReturn(responseList);

        // when & then
        restTestClient.get().uri("/categories")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo(SuccessEnum.READ_SUCCESS.getMessage())
                .jsonPath("$.data[0].name").isEqualTo("전자제품");
    }

    @Test
    void 카테고리_목록_조회_실패_테스트() {
        // given
        given(buyerCategoryService.findAllCategory())
                .willThrow(new BaseException(ErrorEnum.CATEGORY_NOT_FOUND));

        // when & then
        restTestClient.get().uri("/categories")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo(ErrorEnum.CATEGORY_NOT_FOUND.getMessage())
                .jsonPath("$.data").isEmpty();
    }
}
