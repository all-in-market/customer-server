package com.example.allinmarket.buyer.category.controller;

import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.buyer.category.service.BuyerCategoryService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.support.RestDocsControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BuyerCategoryController.class)
public class BuyerCategoryControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private BuyerCategoryService buyerCategoryService;

    @Test
    void 카테고리_목록_조회_성공_테스트() throws Exception {
        CategoryDetailResponse response = new CategoryDetailResponse(1L, "전자제품", 1);
        given(buyerCategoryService.findAllCategory()).willReturn(List.of(response));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data[0].name").value("전자제품"));
    }

    @Test
    void 카테고리_목록_조회_실패_테스트() throws Exception {
        given(buyerCategoryService.findAllCategory())
                .willThrow(new BaseException(ErrorEnum.CATEGORY_NOT_FOUND));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorEnum.CATEGORY_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
