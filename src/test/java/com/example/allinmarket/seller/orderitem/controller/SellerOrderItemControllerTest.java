package com.example.allinmarket.seller.orderitem.controller;

import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.orderitem.dto.OrderItemDetailResponse;
import com.example.allinmarket.seller.orderitem.service.SellerOrderItemService;
import com.example.allinmarket.support.RestDocsControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SellerOrderItemController.class)
public class SellerOrderItemControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private SellerOrderItemService sellerOrderItemService;

    @BeforeEach
    void setAuth() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void 판매자_주문상품목록_조회_성공_테스트() throws Exception {
        OrderItemDetailResponse item = new OrderItemDetailResponse(
                1L, 10L, 100L, 1L, "신발", BigDecimal.valueOf(30000), 1
        );
        PageResponse<OrderItemDetailResponse> pageResponse = new PageResponse<>(
                List.of(item), 1, 1, 1L, 10, true
        );
        when(sellerOrderItemService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/seller/orderitems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].orderId").value(10))
                .andExpect(jsonPath("$.data.content[0].productName").value("신발"))
                .andExpect(jsonPath("$.data.content[0].unitPrice").value(30000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.currentPage").value(1))
                .andDo(document("seller/orderitem/list",
                        queryParameters(
                                parameterWithName("page").optional().description("페이지 번호 (0부터 시작, 기본값: 0)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본값: 10)")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.content[].id").description("주문 상품 ID"),
                                fieldWithPath("data.content[].orderId").description("주문 ID"),
                                fieldWithPath("data.content[].productId").description("상품 ID"),
                                fieldWithPath("data.content[].sellerId").description("판매자 ID"),
                                fieldWithPath("data.content[].productName").description("상품명"),
                                fieldWithPath("data.content[].unitPrice").description("단가"),
                                fieldWithPath("data.content[].quantity").description("수량"),
                                fieldWithPath("data.currentPage").description("현재 페이지 번호"),
                                fieldWithPath("data.totalPages").description("전체 페이지 수"),
                                fieldWithPath("data.totalElements").description("전체 항목 수"),
                                fieldWithPath("data.size").description("페이지 크기"),
                                fieldWithPath("data.isLast").description("마지막 페이지 여부"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 판매자_주문상품목록_조회_빈목록_성공_테스트() throws Exception {
        PageResponse<OrderItemDetailResponse> emptyPage = new PageResponse<>(
                List.of(), 1, 0, 0L, 10, true
        );
        when(sellerOrderItemService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/seller/orderitems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void 판매자_주문상품목록_조회_페이징_파라미터_성공_테스트() throws Exception {
        PageResponse<OrderItemDetailResponse> pageResponse = new PageResponse<>(
                List.of(), 2, 5, 42L, 10, false
        );
        when(sellerOrderItemService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/seller/orderitems?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentPage").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(42))
                .andExpect(jsonPath("$.data.isLast").value(false));
    }
}
