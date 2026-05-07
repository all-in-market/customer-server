package com.example.allinmarket.buyer.product.controller;

import com.example.allinmarket.buyer.product.service.BuyerProductService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.support.RestDocsControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BuyerProductController.class)
public class BuyerProductControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private BuyerProductService buyerProductService;

    @Test
    @WithMockUser
    void 구매자_상품_목록_조회_성공_테스트() throws Exception {
        ProductDetailResponse response = new ProductDetailResponse(
                1L, null, null, "테스트", BigDecimal.valueOf(10000), 50, ProductStatus.ON_SALE, "설명"
        );
        when(buyerProductService.findAllProducts(any(Pageable.class), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("데이터 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.data.content[0].name").value("테스트"))
                .andExpect(jsonPath("$.data.content[0].price").value(10000))
                .andExpect(jsonPath("$.data.content[0].stock").value(50))
                .andExpect(jsonPath("$.data.content[0].status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.content[0].description").value("설명"))
                .andDo(document("buyer/product/list",
                        queryParameters(
                                parameterWithName("categoryId").optional().description("카테고리 ID 필터"),
                                parameterWithName("page").optional().description("페이지 번호 (0부터 시작, 기본값: 0)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본값: 20)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.content[].id").description("상품 ID"),
                                fieldWithPath("data.content[].sellerId").optional().description("판매자 ID"),
                                fieldWithPath("data.content[].categoryId").optional().description("카테고리 ID"),
                                fieldWithPath("data.content[].name").description("상품명"),
                                fieldWithPath("data.content[].price").description("판매 가격"),
                                fieldWithPath("data.content[].stock").description("재고 수량"),
                                fieldWithPath("data.content[].status").description("상품 상태 (ON_SALE: 판매 중)"),
                                fieldWithPath("data.content[].description").description("상품 설명"),
                                fieldWithPath("data.totalElements").description("전체 상품 수"),
                                fieldWithPath("data.totalPages").description("전체 페이지 수"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 구매자_상품_목록_조회_500에러_실패_테스트() throws Exception {
        when(buyerProductService.findAllProducts(any(Pageable.class), any()))
                .thenThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

        mockMvc.perform(get("/products"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser
    void 상품_상세_조회_성공_테스트() throws Exception {
        ProductDetailResponse response = new ProductDetailResponse(
                1L, null, null, "상품 테스트", BigDecimal.valueOf(12000), 30, ProductStatus.ON_SALE, "상품 설명"
        );
        given(buyerProductService.findOneProduct(any())).willReturn(response);

        mockMvc.perform(get("/products/{productId}", response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.name").value("상품 테스트"))
                .andExpect(jsonPath("$.data.price").value(12000))
                .andExpect(jsonPath("$.data.stock").value(30))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.description").value("상품 설명"))
                .andDo(document("buyer/product/detail",
                        pathParameters(
                                parameterWithName("productId").description("조회할 상품 ID")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("상품 ID"),
                                fieldWithPath("data.sellerId").optional().description("판매자 ID"),
                                fieldWithPath("data.categoryId").optional().description("카테고리 ID"),
                                fieldWithPath("data.name").description("상품명"),
                                fieldWithPath("data.price").description("판매 가격"),
                                fieldWithPath("data.stock").description("재고 수량"),
                                fieldWithPath("data.status").description("상품 상태"),
                                fieldWithPath("data.description").description("상품 설명"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 상품_상세_조회_실패_테스트() throws Exception {
        given(buyerProductService.findOneProduct(any()))
                .willThrow(new BaseException(ErrorEnum.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorEnum.PRODUCT_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
