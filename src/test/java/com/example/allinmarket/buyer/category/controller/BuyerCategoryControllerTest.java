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
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
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
                .andExpect(jsonPath("$.data[0].name").value("전자제품"))
                .andDo(document("buyer/category/list",
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data[].id").description("카테고리 ID"),
                                fieldWithPath("data[].name").description("카테고리 이름"),
                                fieldWithPath("data[].sortOrder").description("카테고리 정렬 순서"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
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
