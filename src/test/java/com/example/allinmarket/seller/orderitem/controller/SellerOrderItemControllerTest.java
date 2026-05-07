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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(jsonPath("$.data.currentPage").value(1));
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
