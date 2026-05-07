package com.example.allinmarket.seller.settlement.controller;

import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.settlement.dto.response.SettlementDetailResponse;
import com.example.allinmarket.domain.settlement.enums.SettlementStatus;
import com.example.allinmarket.domain.settlement.enums.SettlementType;
import com.example.allinmarket.seller.settlement.service.SellerSettlementService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SellerSettlementController.class)
class SellerSettlementControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private SellerSettlementService sellerSettlementService;

    @BeforeEach
    void setAuth() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void 판매자_정산내역_조회_성공_테스트() throws Exception {
        SettlementDetailResponse response = new SettlementDetailResponse(
                1L, 1L,
                BigDecimal.valueOf(50000), BigDecimal.valueOf(1000),
                SettlementStatus.COMPLETED, SettlementType.MID,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15),
                null
        );
        PageResponse<SettlementDetailResponse> pageResponse = new PageResponse<>(
                List.of(response), 1, 1, 1L, 10, true
        );
        when(sellerSettlementService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/seller/settlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].amount").value(50000))
                .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.currentPage").value(1));
    }

    @Test
    void 판매자_정산내역_조회_빈목록_성공_테스트() throws Exception {
        PageResponse<SettlementDetailResponse> emptyPage = new PageResponse<>(
                List.of(), 1, 0, 0L, 10, true
        );
        when(sellerSettlementService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/seller/settlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void 판매자_정산내역_조회_페이징_파라미터_성공_테스트() throws Exception {
        PageResponse<SettlementDetailResponse> pageResponse = new PageResponse<>(
                List.of(), 2, 5, 42L, 10, false
        );
        when(sellerSettlementService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/seller/settlements?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentPage").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(42))
                .andExpect(jsonPath("$.data.isLast").value(false));
    }
}
