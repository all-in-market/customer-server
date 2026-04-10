package com.example.allinmarket.buyer.product.controller;


import com.example.allinmarket.buyer.entity.product.controller.BuyerProductController;
import com.example.allinmarket.buyer.entity.product.service.BuyerProductService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuyerProductController.class)
public class BuyerProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private BuyerProductService buyerProductService;

    @Test
    @WithMockUser
    void 구매자_상품_목록_조회_성공_테스트() throws Exception {
        // given
        ProductDetailResponse response = new ProductDetailResponse(
                null,
                null,
                "테스트",
                BigDecimal.valueOf(10000),
                50,
                ProductStatus.ON_SALE,
                "설명"
        );

        when(buyerProductService.findAllProducts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        // when & then
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("데이터 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.data.content[0].name").value("테스트"))
                .andExpect(jsonPath("$.data.content[0].price").value(10000))
                .andExpect(jsonPath("$.data.content[0].stock").value(50))
                .andExpect(jsonPath("$.data.content[0].status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.content[0].description").value("설명"));
    }

    @Test
    void 구매자_상품_목록_조회_500에러_실패_테스트() throws Exception {
        // given
        when(buyerProductService.findAllProducts(Pageable.unpaged()))
                .thenThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

        // when & then: 500 에러 응답 검증
        mockMvc.perform(get("/products")).andExpect(status().isInternalServerError());

    }
}
